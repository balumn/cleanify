package com.cleanify.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.cleanify.cleanup.CleanupController
import com.cleanify.data.AppRuntimeStateRepository
import com.cleanify.data.cleanifyDataStore
import java.util.ArrayDeque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * CleanifyAccessibilityService
 *
 * This AccessibilityService performs UI automation required to force-stop apps on an
 * unrooted device.
 *
 * High-level flow (state machine):
 *
 * 1) Dashboard sends a broadcast with a queue of selected package names.
 * 2) Service opens that package's "App info" (ACTION_APPLICATION_DETAILS_SETTINGS).
 * 3) When the Settings UI is visible, service finds the "Force stop" button node and clicks it.
 * 4) When the confirmation dialog appears, service finds the "OK" button and clicks it.
 * 5) Service polls [ApplicationInfo.FLAG_STOPPED] until the system reports the same state as a
 *    manual App info → Force stop. Only then does it advance (strict policy).
 * 6) If verification fails, it reopens app info and retries the flow (bounded); otherwise it
 *    skips that package and continues the queue.
 * 7) Service navigates back and advances to the next package until the queue is empty.
 * 8) Service updates CleanupController so the Compose UI can show progress and a completion message.
 *
 * Important OEM caveat:
 * Android Settings UIs are not fully standardized across OEM skins.
 * Node matching here is defensive (text/contentDescription + case-insensitive keywords),
 * but you may need to tune selectors for a specific device.
 */
class CleanifyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "Cleanify"
        /** How long to wait for the Force stop row to appear after opening app info. */
        private const val FORCE_STOP_TIMEOUT_MS = 12_000L

        /** How long to wait for the confirmation dialog OK button. */
        private const val OK_TIMEOUT_MS = 12_000L

        /**
         * After tapping OK, we poll [ApplicationInfo.FLAG_STOPPED] this many times before
         * treating the stop as failed and retrying or giving up.
         */
        private const val VERIFY_STOPPED_MAX_POLLS = 12

        private const val VERIFY_STOPPED_POLL_INTERVAL_MS = 400L

        /** First poll delay so the system can apply the force-stop before we read flags. */
        private const val VERIFY_STOPPED_INITIAL_DELAY_MS = 300L

        /**
         * How many times to reopen app info and re-run the Force stop flow for one package
         * when verification says the app is still not stopped.
         */
        private const val MAX_FULL_AUTOMATION_RETRIES_PER_PACKAGE = 4

        private val FORCE_STOP_REGEX = Regex("force[-\\s]?stop", RegexOption.IGNORE_CASE)

        private val OK_KEYWORDS = listOf(
            // Common English variants (case-insensitive match below).
            "ok",
            "okay",
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private val processLock = Any()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appRuntimeStateRepository by lazy {
        AppRuntimeStateRepository(applicationContext.cleanifyDataStore)
    }

    private var receiverRegistered = false
    private var startReceiver: BroadcastReceiver? = null

    private enum class Stage {
        Idle,
        WaitingForForceStop,
        WaitingForOkDialog,
        NavigatingBack,
    }

    private sealed interface ForceStopResolution {
        data class Clickable(val node: AccessibilityNodeInfo) : ForceStopResolution
        data object AlreadyStopped : ForceStopResolution
    }

    @Volatile
    private var stage: Stage = Stage.Idle

    private var queue: List<String> = emptyList()
    private var index: Int = 0

    private var forceStopTimeoutRunnable: Runnable? = null
    private var okTimeoutRunnable: Runnable? = null
    private var verifyStoppedRunnable: Runnable? = null

    /** Re-open app info + automation attempts after OK did not result in FLAG_STOPPED. */
    private var fullAutomationRetriesForCurrentPackage: Int = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected")

        // Register for cleanup requests coming from the dashboard.
        // We register dynamically so the app doesn't need a manifest receiver.
        if (!receiverRegistered) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action != CleanupController.ACTION_START_CLEANUP) return

                    val packages = intent.getStringArrayListExtra(CleanupController.EXTRA_PACKAGE_LIST)
                        ?.filter { !it.isNullOrBlank() }
                        ?.distinct()
                        .orEmpty()

                    if (packages.isEmpty()) {
                        CleanupController.onCleanupFailed("Queue is empty.")
                        return
                    }

                    // Reset and start chain.
                    queue = packages
                    index = 0
                    stage = Stage.WaitingForForceStop

                    cancelTimeouts()
                    openAppInfoForPackage(queue[index])
                }
            }

            startReceiver = receiver
            val filter = android.content.IntentFilter(CleanupController.ACTION_START_CLEANUP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(receiver, filter)
            }
            receiverRegistered = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // The Accessibility framework invokes this for the configured event types.
        // We operate only when we are inside an expected stage.
        val currentStage = stage
        if (currentStage == Stage.Idle) return

        // Only react to relevant UI updates.
        val eventType = event?.eventType ?: -1
        if (
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val root = rootInActiveWindow ?: return

        // Serialize handling: a naive "busy" flag dropped events during fast UI transitions,
        // which caused missed Force stop / OK clicks. A lock preserves every callback.
        synchronized(processLock) {
            try {
                if (currentStage == Stage.WaitingForForceStop) {
                    // We are waiting for the "Force stop" button on the app details page.
                    when (val resolution = resolveForceStop(root)) {
                        is ForceStopResolution.Clickable -> {
                            Log.d(TAG, "Found Force stop node; clicking.")
                            stage = Stage.WaitingForOkDialog
                            cancelForceStopTimeout()
                            if (resolution.node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                scheduleOkTimeout()
                            } else {
                                Log.w(TAG, "Force stop click performAction returned false.")
                            }
                        }
                        is ForceStopResolution.AlreadyStopped -> {
                            Log.d(TAG, "System reports app already force-stopped; advancing.")
                            markCurrentPackageStopped()
                            skipToNextPackage(
                                reason = "App already stopped.",
                                shouldAttemptBack = true,
                            )
                        }
                        null -> Unit
                    }
                } else if (currentStage == Stage.WaitingForOkDialog) {
                    // Confirmation dialog is visible; find and click OK.
                    val okNode = findOkNode(root)
                    if (okNode != null) {
                        Log.d(TAG, "Found OK node; clicking.")
                        stage = Stage.NavigatingBack
                        cancelOkTimeout()

                        if (okNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            scheduleVerifyStoppedAndMaybeAdvance()
                        } else {
                            Log.w(TAG, "OK click performAction returned false.")
                            scheduleOkTimeout()
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "onAccessibilityEvent error", t)
                CleanupController.onCleanupFailed(t.message ?: "Accessibility error.")
                stage = Stage.Idle
                cancelTimeouts()
            }
        }
    }

    private fun openAppInfoForPackage(
        packageName: String,
        resetFullAutomationRetries: Boolean = true,
        abortEntireRunOnOpenFailure: Boolean = true,
    ) {
        Log.d(TAG, "Opening app info for $packageName")
        if (resetFullAutomationRetries) {
            fullAutomationRetriesForCurrentPackage = 0
        }

        val skipReason = shouldSkipCleanup(packageName)
        if (skipReason != null) {
            Log.d(TAG, "Skipping cleanup for $packageName: $skipReason")
            markCurrentPackageStopped()
            skipToNextPackage(
                reason = skipReason,
                shouldAttemptBack = false,
            )
            return
        }

        // Intent to open "App info" screen for the given package.
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to open app settings for $packageName", t)
            if (abortEntireRunOnOpenFailure) {
                CleanupController.onCleanupFailed(
                    "Failed to open app settings: ${t.message ?: "unknown"}",
                )
                stage = Stage.Idle
            } else {
                skipToNextPackage(
                    reason = "Failed to reopen app settings.",
                    shouldAttemptBack = true,
                )
            }
            return
        }

        // Update progress immediately (UI will show "Stopping app i of n").
        CleanupController.onAppStopped(
            nextIndex = index,
            total = queue.size,
            nextLabel = queue.getOrNull(index).orEmpty(),
        )

        scheduleForceStopTimeout()
    }

    /**
     * After the user confirms Force stop, poll until [ApplicationInfo.FLAG_STOPPED] is set.
     * If it never becomes set, reopen app info and retry the flow up to [MAX_FULL_AUTOMATION_RETRIES_PER_PACKAGE].
     */
    private fun scheduleVerifyStoppedAndMaybeAdvance() {
        cancelVerifyStoppedRunnable()
        var pollCount = 0

        verifyStoppedRunnable = object : Runnable {
            override fun run() {
                if (stage != Stage.NavigatingBack) return

                val pkg = queue.getOrNull(index).orEmpty()
                if (pkg.isBlank()) {
                    cancelVerifyStoppedRunnable()
                    return
                }

                if (isAppInForceStoppedState(pkg)) {
                    cancelVerifyStoppedRunnable()
                    Log.d(TAG, "Verified FLAG_STOPPED for $pkg; advancing queue.")
                    advanceQueueAfterVerifiedStop()
                    return
                }

                pollCount++
                if (pollCount >= VERIFY_STOPPED_MAX_POLLS) {
                    cancelVerifyStoppedRunnable()
                    handleUnverifiedForceStopAttempt(pkg)
                    return
                }

                handler.postDelayed(this, VERIFY_STOPPED_POLL_INTERVAL_MS)
            }
        }
        handler.postDelayed(verifyStoppedRunnable!!, VERIFY_STOPPED_INITIAL_DELAY_MS)
    }

    private fun handleUnverifiedForceStopAttempt(packageName: String) {
        fullAutomationRetriesForCurrentPackage++
        Log.w(
            TAG,
            "Force stop not verified for $packageName (attempt $fullAutomationRetriesForCurrentPackage/$MAX_FULL_AUTOMATION_RETRIES_PER_PACKAGE)",
        )

        if (fullAutomationRetriesForCurrentPackage >= MAX_FULL_AUTOMATION_RETRIES_PER_PACKAGE) {
            skipToNextPackage(
                reason = "Could not verify force stop after retries.",
                shouldAttemptBack = true,
            )
            return
        }

        stage = Stage.WaitingForForceStop
        cancelTimeouts()
        handler.postDelayed({
            openAppInfoForPackage(
                packageName,
                resetFullAutomationRetries = false,
                abortEntireRunOnOpenFailure = false,
            )
        }, 450L)
    }

    private fun advanceQueueAfterVerifiedStop() {
        if (queue.isEmpty()) return

        markCurrentPackageStopped()

        // Move index forward and either open the next app info screen or finish.
        val nextIndex = index + 1

        // We always attempt to go back to a stable state.
        // Using GLOBAL_ACTION_BACK is the most reliable generic action we have here.
        performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)

        if (nextIndex >= queue.size) {
            stage = Stage.Idle
            cancelTimeouts()
            CleanupController.onAllDone()

            // Return user to dashboard.
            // We use NEW_TASK because we're not running inside an activity.
            val intent = Intent(this, com.cleanify.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } else {
            index = nextIndex
            stage = Stage.WaitingForForceStop
            cancelTimeouts()

            openAppInfoForPackage(queue[index])
        }
    }

    private fun scheduleForceStopTimeout(timeoutMs: Long = FORCE_STOP_TIMEOUT_MS) {
        cancelForceStopTimeout()
        forceStopTimeoutRunnable = Runnable {
            Log.w(TAG, "Force stop timeout; skipping package index=$index")
            skipToNextPackage(
                reason = "Timed out waiting for Force stop.",
                shouldAttemptBack = false,
            )
        }
        handler.postDelayed(forceStopTimeoutRunnable!!, timeoutMs)
    }

    private fun scheduleOkTimeout(timeoutMs: Long = OK_TIMEOUT_MS) {
        cancelOkTimeout()
        okTimeoutRunnable = Runnable {
            Log.w(TAG, "OK timeout; skipping package index=$index")
            skipToNextPackage(
                reason = "Timed out waiting for OK confirmation.",
                shouldAttemptBack = true,
            )
        }
        handler.postDelayed(okTimeoutRunnable!!, timeoutMs)
    }

    /**
     * Skip the current item and continue with the next one.
     *
     * The requirements emphasize chaining through the entire queue; timeouts should not
     * stop the whole cleanup run.
     */
    private fun skipToNextPackage(
        reason: String,
        shouldAttemptBack: Boolean,
    ) {
        if (queue.isEmpty()) return

        cancelTimeouts()

        // Optional: if we're stuck in a dialog, try to exit it first.
        if (shouldAttemptBack) {
            performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        }

        val nextIndex = index + 1
        if (nextIndex >= queue.size) {
            stage = Stage.Idle
            CleanupController.onAllDone()
            val intent = Intent(this, com.cleanify.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            return
        }

        // Advance index and restart the chain from the next package.
        index = nextIndex
        stage = Stage.WaitingForForceStop

        handler.postDelayed({
            // openAppInfoForPackage updates CleanupController UI state immediately.
            openAppInfoForPackage(queue[index])
        }, if (shouldAttemptBack) 300 else 0)
    }

    private fun cancelForceStopTimeout() {
        forceStopTimeoutRunnable?.let { handler.removeCallbacks(it) }
        forceStopTimeoutRunnable = null
    }

    private fun cancelOkTimeout() {
        okTimeoutRunnable?.let { handler.removeCallbacks(it) }
        okTimeoutRunnable = null
    }

    private fun cancelVerifyStoppedRunnable() {
        verifyStoppedRunnable?.let { handler.removeCallbacks(it) }
        verifyStoppedRunnable = null
    }

    private fun cancelTimeouts() {
        cancelForceStopTimeout()
        cancelOkTimeout()
        cancelVerifyStoppedRunnable()
    }

    private fun isLikelySettingsUi(event: AccessibilityEvent?): Boolean {
        val pkg = event?.packageName?.toString().orEmpty()
        // AOSP often uses com.android.settings; keep an inclusive check to support OEMs.
        return pkg.contains("settings", ignoreCase = true) || pkg == "com.android.settings"
    }

    private fun isLikelyDialogUi(root: AccessibilityNodeInfo): Boolean {
        // Heuristic: dialogs often contain actionable "OK" nodes near the bottom.
        // We keep this permissive; real matching is performed by findOkNode().
        return findTextNode(root, "ok") != null || findTextNode(root, "okay") != null
    }

    /**
     * Finds a node representing the "Force stop" button.
     *
     * We scan the active window tree looking for either:
     * - node.text matching /force[- ]?stop/i
     * - node.contentDescription matching the same keyword
     *
     * Because some node hierarchies place the clickable parent above the text node,
     * we return the clickable ancestor when needed.
     */
    private fun resolveForceStop(root: AccessibilityNodeInfo): ForceStopResolution? {
        val nodeQueue: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        nodeQueue.add(root)

        while (nodeQueue.isNotEmpty()) {
            val node = nodeQueue.removeFirst()
            try {
                val text = node.text?.toString().orEmpty()
                val desc = node.contentDescription?.toString().orEmpty()
                val combined = "$text $desc"

                if (FORCE_STOP_REGEX.containsMatchIn(combined)) {
                    val clickableAncestor = findClickableAncestor(node)
                    if (clickableAncestor != null && clickableAncestor.isEnabled) {
                        return ForceStopResolution.Clickable(clickableAncestor)
                    }
                    // Do not assume "already stopped" from a greyed label alone: the tree may
                    // still be loading, or the clickable parent may differ on OEM skins.
                    val pkg = queue.getOrNull(index).orEmpty()
                    if (pkg.isNotBlank() && isAppInForceStoppedState(pkg)) {
                        return ForceStopResolution.AlreadyStopped
                    }
                    return null
                }

                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { nodeQueue.add(it) }
                }
            } catch (_: Throwable) {
                // Ignore transient nodes that might become invalid during transitions.
            }
        }

        return null
    }

    /**
     * Finds the "OK" button node in the confirmation dialog.
     */
    private fun findOkNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            try {
                val text = node.text?.toString().orEmpty().trim().lowercase()
                val desc = node.contentDescription?.toString().orEmpty().trim().lowercase()

                if (OK_KEYWORDS.any { keyword ->
                        (text == keyword) || desc.contains(keyword, ignoreCase = true)
                    }
                ) {
                    return findClickableAncestor(node)
                }

                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }
            } catch (_: Throwable) {
                // Ignore nodes that disappear during transitions.
            }
        }

        return null
    }

    /**
     * Searches for a text keyword node without requiring it to be clickable.
     * Used for heuristics only.
     */
    private fun findTextNode(root: AccessibilityNodeInfo, keyword: String): AccessibilityNodeInfo? {
        val queue: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            try {
                val text = node.text?.toString().orEmpty().trim().lowercase()
                if (text == keyword.lowercase()) return node
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }
            } catch (_: Throwable) {
            }
        }
        return null
    }

    /**
     * Walks upward from a node to find the nearest clickable ancestor.
     *
     * In many Settings UIs, a text node isn't clickable directly, but its parent is.
     * Clicking the parent is usually the correct automation strategy.
     */
    private fun findClickableAncestor(start: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = start
        while (current != null) {
            try {
                if (current.isClickable) return current
                current = current.parent
            } catch (_: Throwable) {
                return null
            }
        }
        return null
    }

    private fun shouldSkipCleanup(packageName: String): String? {
        if (packageName == this.packageName) {
            return "Cannot force stop Cleanify."
        }
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            if (!appInfo.enabled) {
                return "App is disabled."
            }
            if (isAppInForceStoppedState(packageName)) {
                return "App already stopped."
            }
            null
        }.getOrElse { t ->
            // If package info cannot be resolved, don't block cleanup attempts.
            Log.w(TAG, "Unable to inspect app state for $packageName", t)
            null
        }
    }

    /**
     * True when the package is in the same "force stopped" state as after using App info → Force stop
     * (has not been launched since the stop).
     */
    private fun isAppInForceStoppedState(packageName: String): Boolean {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_STOPPED) != 0
        }.getOrDefault(false)
    }

    private fun markCurrentPackageStopped() {
        val packageName = queue.getOrNull(index).orEmpty()
        if (packageName.isBlank()) return
        serviceScope.launch {
            appRuntimeStateRepository.markStopped(packageName)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt")
        stage = Stage.Idle
        cancelTimeouts()
        CleanupController.onCleanupFailed("Accessibility service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (receiverRegistered) {
                startReceiver?.let { unregisterReceiver(it) }
            }
        } catch (_: Throwable) {
        }
        stage = Stage.Idle
        cancelTimeouts()
        serviceScope.cancel()
    }
}

