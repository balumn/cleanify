package com.cleanify.feature.selection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanify.R
import com.cleanify.data.model.AppListItem
import com.cleanify.ui.common.DrawableIcon
import com.cleanify.ui.theme.CleanifyScreenBackground

@Composable
fun AppSelectionScreen(
    onNavigateBack: () -> Unit,
) {
    val vm: AppSelectionViewModel = viewModel()
    val apps by vm.apps.collectAsState()
    val selected by vm.selectedPackages.collectAsState()
    val loadState by vm.loadState.collectAsState()

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.select_apps_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    val error = remember { mutableStateOf<String?>(null) }
                    error.value?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Button(
                        onClick = {
                            vm.saveAndReturn(
                                onSaved = onNavigateBack,
                                onError = { msg -> error.value = msg },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 6.dp,
                        ),
                    ) {
                        Text(
                            stringResource(R.string.save_selection),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        CleanifyScreenBackground(
            modifier = Modifier.padding(innerPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnimatedContent(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        targetState = loadState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(280, delayMillis = 20)) togetherWith
                                fadeOut(animationSpec = tween(200))
                        },
                        label = "selection_load",
                    ) { state ->
                        when (state) {
                        AppSelectionLoadState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(44.dp),
                                        strokeWidth = 4.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Text(
                                        text = stringResource(R.string.indexing_message),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        is AppSelectionLoadState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                ),
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Text(
                                    modifier = Modifier.padding(18.dp),
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        is AppSelectionLoadState.Loaded -> {
                            val (userApps, systemApps) = remember(apps, selected) {
                                val userSelected = mutableListOf<AppListItem>()
                                val userUnselected = mutableListOf<AppListItem>()
                                val systemSelected = mutableListOf<AppListItem>()
                                val systemUnselected = mutableListOf<AppListItem>()

                                apps.forEach { app ->
                                    val isSelected = selected.contains(app.packageName)
                                    when {
                                        app.isSystem && isSelected -> systemSelected += app
                                        app.isSystem -> systemUnselected += app
                                        isSelected -> userSelected += app
                                        else -> userUnselected += app
                                    }
                                }

                                (userSelected + userUnselected) to (systemSelected + systemUnselected)
                            }
                            var userSectionExpanded by rememberSaveable { mutableStateOf(true) }
                            var systemSectionExpanded by rememberSaveable { mutableStateOf(true) }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (userApps.isNotEmpty()) {
                                    item(key = "user_header") {
                                        SectionHeader(
                                            title = stringResource(R.string.user_apps_section_title),
                                            expanded = userSectionExpanded,
                                            count = userApps.size,
                                            allSelected = userApps.all { selected.contains(it.packageName) },
                                            onSelectAllToggle = { shouldSelectAll ->
                                                userApps.forEach { app ->
                                                    vm.togglePackage(app.packageName, shouldSelectAll)
                                                }
                                            },
                                            onToggle = { userSectionExpanded = !userSectionExpanded },
                                        )
                                    }
                                    if (userSectionExpanded) {
                                        items(
                                            items = userApps,
                                            key = { it.packageName },
                                        ) { app ->
                                            AppRow(
                                                app = app,
                                                checked = selected.contains(app.packageName),
                                                onCheckedChange = { enabled ->
                                                    vm.togglePackage(app.packageName, enabled)
                                                },
                                            )
                                        }
                                    }
                                }

                                if (systemApps.isNotEmpty()) {
                                    item(key = "system_header") {
                                        SectionHeader(
                                            title = stringResource(R.string.system_apps_section_title),
                                            expanded = systemSectionExpanded,
                                            count = systemApps.size,
                                            allSelected = systemApps.all { selected.contains(it.packageName) },
                                            onSelectAllToggle = { shouldSelectAll ->
                                                systemApps.forEach { app ->
                                                    vm.togglePackage(app.packageName, shouldSelectAll)
                                                }
                                            },
                                            onToggle = { systemSectionExpanded = !systemSectionExpanded },
                                        )
                                    }
                                    if (systemSectionExpanded) {
                                        items(
                                            items = systemApps,
                                            key = { it.packageName },
                                        ) { app ->
                                            AppRow(
                                                app = app,
                                                checked = selected.contains(app.packageName),
                                                onCheckedChange = { enabled ->
                                                    vm.togglePackage(app.packageName, enabled)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    count: Int,
    allSelected: Boolean,
    onSelectAllToggle: (Boolean) -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Checkbox(
                checked = allSelected,
                onCheckedChange = { checked -> onSelectAllToggle(checked) },
            )
            Text(
                text = "$title ($count)",
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = (-0.1).sp,
                ),
                fontWeight = FontWeight.SemiBold,
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) {
                    stringResource(R.string.collapse)
                } else {
                    stringResource(R.string.expand)
                },
            )
        }
    }
}

@Composable
private fun AppRow(
    app: AppListItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    DrawableIcon(
                        drawable = app.icon,
                        contentDescription = app.label,
                        size = 40.dp,
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
                if (app.isSystem) {
                    Text(
                        text = stringResource(R.string.system_app),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
