package com.cleanify.cleanup

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-process state machine for cleanup.
 *
 * The AccessibilityService performs the UI automation. This controller keeps the queue
 * and exposes progress state to the Compose UI.
 *
 * The service will update this state via [onAppStopped] / [onAllDone] / [onCleanupFailed].
 */
object CleanupController {
    const val ACTION_START_CLEANUP = "com.cleanify.ACTION_START_CLEANUP"
    const val EXTRA_PACKAGE_LIST = "com.cleanify.EXTRA_PACKAGE_LIST"

    sealed interface CleanupUiState {
        data object Idle : CleanupUiState
        data class Running(
            val index: Int,
            val total: Int,
            val currentLabel: String,
        ) : CleanupUiState
        data object Done : CleanupUiState
        data class Failed(val reason: String) : CleanupUiState
    }

    private val _uiState = MutableStateFlow<CleanupUiState>(CleanupUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val queue = CopyOnWriteArrayList<String>()

    /**
     * Called by the dashboard when the user taps "Clean Up".
     *
     * This sends a broadcast for the AccessibilityService to pick up and start the chain.
     */
    fun startCleanup(
        context: Context,
        packageNames: List<String>,
    ) {
        val ownPackage = context.packageName
        val sanitized = packageNames
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { it != ownPackage }
            .distinct()
            .toList()

        if (sanitized.isEmpty()) {
            _uiState.value = CleanupUiState.Failed("No apps selected.")
            return
        }

        queue.clear()
        queue.addAll(sanitized)

        val first = sanitized.firstOrNull().orEmpty()
        _uiState.value = CleanupUiState.Running(
            index = 0,
            total = sanitized.size,
            currentLabel = first,
        )

        val intent = Intent(ACTION_START_CLEANUP).apply {
            setPackage(context.packageName) // make it explicit within the app
            putStringArrayListExtra(EXTRA_PACKAGE_LIST, ArrayList(sanitized))
        }
        context.sendBroadcast(intent)
    }

    /**
     * Called by the AccessibilityService after it has successfully stopped the current app
     * and is about to move forward.
     */
    fun onAppStopped(nextIndex: Int, total: Int, nextLabel: String) {
        _uiState.value = CleanupUiState.Running(
            index = nextIndex.coerceIn(0, total.coerceAtLeast(1) - 1),
            total = total,
            currentLabel = nextLabel,
        )
    }

    fun onAllDone() {
        _uiState.value = CleanupUiState.Done
        queue.clear()
    }

    fun onCleanupFailed(reason: String) {
        _uiState.value = CleanupUiState.Failed(reason)
        queue.clear()
    }
}

