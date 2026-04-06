package com.cleanify.feature.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanify.ui.common.DrawableIcon
import com.cleanify.R
import com.cleanify.cleanup.CleanupController
import com.cleanify.core.accessibility.AccessibilityUtils
import com.cleanify.ui.theme.CleanifyScreenBackground
import kotlin.random.Random
import kotlinx.coroutines.launch

private enum class DashboardPhase {
    Indexing,
    Empty,
    List,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onEditApps: () -> Unit,
) {
    val viewModel: DashboardViewModel = viewModel()
    val selectedApps = viewModel.selectedApps.collectAsState().value
    val runningApps = viewModel.runningApps.collectAsState().value
    val stoppedApps = viewModel.stoppedApps.collectAsState().value
    val isIndexing = viewModel.isIndexing.collectAsState().value
    val cleanupUiState = CleanupController.uiState.collectAsState().value

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isServiceEnabled by remember {
        mutableStateOf(AccessibilityUtils.isCleanifyAccessibilityServiceEnabled(context))
    }
    val cleanupCompleteMessage = context.getString(R.string.cleanup_complete)
    val cleanupFailedPrefix = context.getString(R.string.cleanup_failed)

    val snackbarHostState = remember { SnackbarHostState() }
    var forceCleanupSelection by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(runningApps) {
        val running = runningApps.map { it.packageName }.toSet()
        forceCleanupSelection = forceCleanupSelection.filter { it in running }.toSet()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = AccessibilityUtils.isCleanifyAccessibilityServiceEnabled(context)
                viewModel.refreshAppStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(cleanupUiState) {
        when (cleanupUiState) {
            is CleanupController.CleanupUiState.Done -> {
                forceCleanupSelection = emptySet()
                snackbarHostState.showSnackbar(
                    message = cleanupCompleteMessage,
                )
            }
            is CleanupController.CleanupUiState.Failed -> snackbarHostState.showSnackbar(
                message = "$cleanupFailedPrefix: ${cleanupUiState.reason}",
            )
            else -> Unit
        }
        if (cleanupUiState is CleanupController.CleanupUiState.Done) {
            viewModel.refreshAppStates()
        }
    }

    val cleanupRunning = cleanupUiState is CleanupController.CleanupUiState.Running
    val canCleanup = runningApps.isNotEmpty() &&
        isServiceEnabled &&
        !isIndexing &&
        !cleanupRunning
    val multiSelectActive = forceCleanupSelection.isNotEmpty()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (multiSelectActive) {
                        Text(
                            text = stringResource(
                                R.string.dashboard_selected_count,
                                forceCleanupSelection.size,
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = stringResource(R.string.dashboard_tagline),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (isServiceEnabled) {
                        if (multiSelectActive) {
                            IconButton(
                                onClick = {
                                    val count = forceCleanupSelection.size
                                    val pkgs = forceCleanupSelection.toSet()
                                    viewModel.removePackagesFromSelection(pkgs)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(
                                                R.string.removed_apps_from_list_snackbar,
                                                count,
                                            ),
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlaylistRemove,
                                    contentDescription = stringResource(
                                        R.string.content_desc_remove_selected_from_list,
                                    ),
                                )
                            }
                            IconButton(
                                onClick = {
                                    CleanupController.startCleanup(
                                        context,
                                        forceCleanupSelection.toList(),
                                    )
                                },
                                enabled = canCleanup,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = stringResource(
                                        R.string.content_desc_force_cleanup_selected,
                                    ),
                                )
                            }
                        } else {
                            IconButton(onClick = onEditApps) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.edit_apps),
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.tertiary,
                )
            }
        },
        bottomBar = {
            if (isServiceEnabled) {
                val runningState = cleanupUiState as? CleanupController.CleanupUiState.Running
                val showProgress = runningState != null && runningState.total > 0
                val showCleanUpButton = !multiSelectActive
                if (showProgress || showCleanUpButton) {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                        ) {
                            if (showProgress && runningState != null) {
                                val progress = (runningState.index + 1f) / runningState.total
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            if (showCleanUpButton) {
                                Button(
                                    onClick = {
                                        val packageNames = runningApps.map { it.packageName }
                                        CleanupController.startCleanup(context, packageNames)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    enabled = canCleanup,
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 2.dp,
                                        pressedElevation = 6.dp,
                                        disabledElevation = 0.dp,
                                    ),
                                ) {
                                    when (val state = cleanupUiState) {
                                        is CleanupController.CleanupUiState.Running -> {
                                            Text(
                                                stringResource(
                                                    R.string.stopping_app,
                                                    state.index + 1,
                                                    state.total,
                                                ),
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp),
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                stringResource(R.string.clean_up),
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        CleanifyScreenBackground(
            modifier = Modifier.padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                if (!isServiceEnabled) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AccessibilityRequiredCard(
                            onOpenSettings = { AccessibilityUtils.openAccessibilitySettings(context) },
                        )
                    }
                } else {
                    val phase = when {
                        isIndexing -> DashboardPhase.Indexing
                        selectedApps.isEmpty() -> DashboardPhase.Empty
                        else -> DashboardPhase.List
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        AnimatedContent(
                            modifier = Modifier.fillMaxSize(),
                            targetState = phase,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(300, delayMillis = 40)) +
                                    slideInVertically(animationSpec = tween(320)) { it / 12 }) togetherWith
                                    (fadeOut(animationSpec = tween(200)) +
                                        slideOutVertically(animationSpec = tween(230)) { -it / 18 })
                            },
                            label = "dashboard_phase",
                        ) { target ->
                            when (target) {
                                DashboardPhase.Indexing -> Box(Modifier.fillMaxSize())
                                DashboardPhase.Empty -> EmptyDashboardContent(onEditApps = onEditApps)
                                DashboardPhase.List -> DashboardAppListContent(
                                    runningApps = runningApps,
                                    stoppedApps = stoppedApps,
                                    cleanupUiState = cleanupUiState,
                                    forceCleanupSelection = forceCleanupSelection,
                                    onToggleForceCleanupPackage = { pkg ->
                                        forceCleanupSelection =
                                            if (pkg in forceCleanupSelection) {
                                                forceCleanupSelection - pkg
                                            } else {
                                                forceCleanupSelection + pkg
                                            }
                                    },
                                    onSelectAllRunningForCleanup = {
                                        forceCleanupSelection =
                                            runningApps.map { it.packageName }.toSet()
                                    },
                                    onClearForceCleanupSelection = { forceCleanupSelection = emptySet() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (isIndexing && isServiceEnabled) {
        DashboardFullScreenLoader(
            isIndexing = isIndexing,
            modifier = Modifier.fillMaxSize(),
        )
    }
    }
}

@Composable
private fun DashboardFullScreenLoader(
    isIndexing: Boolean,
    modifier: Modifier = Modifier,
) {
    val messages = stringArrayResource(R.array.dashboard_loading_messages).toList()
    var messageIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isIndexing) {
        if (!isIndexing) return@LaunchedEffect
        if (messages.isEmpty()) return@LaunchedEffect
        messageIndex = Random.nextInt(messages.size)
        while (true) {
            delay(2400)
            messageIndex = (messageIndex + 1) % messages.size
        }
    }

    val transition = rememberInfiniteTransition(label = "full_loader_breathe")
    val breathe by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.78f))
            .pointerInput(Unit) {
                detectTapGestures { }
            },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(88.dp)
                    .scale(breathe),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            if (messages.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 14.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_loading_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.dashboard_loading_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        AnimatedContent(
                            targetState = messageIndex,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(320)) togetherWith
                                    fadeOut(animationSpec = tween(220))
                            },
                            label = "loading_message",
                        ) { idx ->
                            Text(
                                text = messages[idx],
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardAppListContent(
    runningApps: List<DashboardAppItem>,
    stoppedApps: List<DashboardAppItem>,
    cleanupUiState: CleanupController.CleanupUiState,
    forceCleanupSelection: Set<String>,
    onToggleForceCleanupPackage: (String) -> Unit,
    onSelectAllRunningForCleanup: () -> Unit,
    onClearForceCleanupSelection: () -> Unit,
) {
    val context = LocalContext.current
    val alreadyStoppedToastMessage = stringResource(R.string.app_already_stopped)
    val noRunningHappyMessages = stringArrayResource(R.array.no_running_apps_happy_messages)
    val noRunningEmptyMessage = remember {
        noRunningHappyMessages.random()
    }
    var stoppedSectionExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(contentType = "section_header") {
            SectionTitle(text = stringResource(R.string.running_apps_section_title))
        }

        if (runningApps.isEmpty()) {
            item(contentType = "empty_running") {
                SectionEmptyCard(text = noRunningEmptyMessage)
            }
        } else {
            item(contentType = "running_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSelectAllRunningForCleanup) {
                        Text(stringResource(R.string.select_all))
                    }
                    TextButton(
                        onClick = onClearForceCleanupSelection,
                        enabled = forceCleanupSelection.isNotEmpty(),
                    ) {
                        Text(stringResource(R.string.deselect_all))
                    }
                }
            }
            items(
                runningApps,
                key = { it.packageName },
                contentType = { "running_app" },
            ) { app ->
                val selected = app.packageName in forceCleanupSelection
                RunningAppRowItem(
                    app = app,
                    selected = selected,
                    onToggle = { onToggleForceCleanupPackage(app.packageName) },
                )
            }
        }

        item(contentType = "section_header") {
            if (stoppedApps.isEmpty()) {
                SectionTitle(
                    text = stringResource(R.string.stopped_apps_section_title),
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                StoppedAppsCollapsibleSectionHeader(
                    stoppedCount = stoppedApps.size,
                    expanded = stoppedSectionExpanded,
                    onToggleExpanded = { stoppedSectionExpanded = !stoppedSectionExpanded },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        if (stoppedApps.isEmpty()) {
            item(contentType = "empty_stopped") {
                SectionEmptyCard(text = stringResource(R.string.no_stopped_apps))
            }
        } else if (stoppedSectionExpanded) {
            items(
                stoppedApps,
                key = { it.packageName },
                contentType = { "stopped_app" },
            ) { app ->
                ElevatedAppCard {
                    RowAppItem(
                        label = app.label,
                        icon = app.icon,
                        isSystem = app.isSystem,
                        onClick = {
                            Toast.makeText(
                                context,
                                alreadyStoppedToastMessage,
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
            }
        }

        item(contentType = "footer_hint") {
            Text(
                text = stringResource(R.string.running_apps_selection_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun StaggeredVisibility(
    delayMillis: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(450, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
            slideInVertically(animationSpec = tween(450)) { it / 8 },
    ) {
        content()
    }
}

@Composable
private fun EmptyDashboardContent(
    onEditApps: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StaggeredVisibility(delayMillis = 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                        ),
                                    ),
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.cleanifysvg),
                                contentDescription = null,
                            )
                        }
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 18.dp),
                        )
                        Text(
                            text = stringResource(R.string.dashboard_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            text = stringResource(R.string.empty_state_cta_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.no_apps_selected_yet),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }

            StaggeredVisibility(delayMillis = 160) {
                Button(
                    onClick = onEditApps,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_apps_title))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                letterSpacing = (-0.12).sp,
            ),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StoppedAppsCollapsibleSectionHeader(
    stoppedCount: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggleExpanded),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.stopped_apps_section_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = (-0.12).sp,
                ),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.stopped_apps_count_subtitle, stoppedCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ElevatedAppCard(
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        border = BorderStroke(
            width = if (highlighted) 2.dp else 1.dp,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                outline.copy(alpha = 0.35f)
            },
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        content()
    }
}

@Composable
private fun SectionEmptyCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RowAppItem(
    label: String,
    icon: android.graphics.drawable.Drawable?,
    isSystem: Boolean,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = {
                    onLongPress?.invoke() ?: onClick?.invoke()
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            tonalElevation = 0.dp,
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                DrawableIcon(
                    drawable = icon,
                    contentDescription = label,
                    size = 44.dp,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSystem) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
            )
        }

        if (isSystem) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = stringResource(R.string.system_app),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RunningAppRowItem(
    app: DashboardAppItem,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    ElevatedAppCard(highlighted = selected) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = onToggle,
                        onLongClick = onToggle,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    tonalElevation = 0.dp,
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        DrawableIcon(
                            drawable = app.icon,
                            contentDescription = app.label,
                            size = 44.dp,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (app.isSystem) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                    )
                }

                if (app.isSystem) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = stringResource(R.string.system_app),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessibilityRequiredCard(
    onOpenSettings: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.primaryContainer,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(scheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SettingsAccessibility,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = scheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.accessibility_required_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.accessibility_required_body),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
            ) {
                Text(
                    text = stringResource(R.string.quick_start_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary,
                    letterSpacing = 0.8.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                AccessibilityQuickStartLine(stringResource(R.string.quick_start_step_1))
                Spacer(modifier = Modifier.height(8.dp))
                AccessibilityQuickStartLine(stringResource(R.string.quick_start_step_2))
                Spacer(modifier = Modifier.height(8.dp))
                AccessibilityQuickStartLine(stringResource(R.string.quick_start_step_3))
            }
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .padding(top = 22.dp)
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.open_accessibility_settings))
            }
        }
    }
}

@Composable
private fun AccessibilityQuickStartLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp, end = 12.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
