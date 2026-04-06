package com.cleanify.feature.dashboard

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanify.core.SystemAppClassifier
import com.cleanify.data.AppRuntimeStateRepository
import com.cleanify.data.SelectedAppsRepository
import com.cleanify.data.cleanifyDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

data class DashboardAppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
)

class DashboardViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = SelectedAppsRepository(application.cleanifyDataStore)
    private val appRuntimeStateRepository = AppRuntimeStateRepository(application.cleanifyDataStore)

    private val selectedPackages: StateFlow<Set<String>> = repository.selectedPackages
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _uiStoppedPackages = MutableStateFlow<Set<String>>(emptySet())

    private val _isIndexing = MutableStateFlow(true)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    /**
     * Built together with running/stopped on each refresh so the UI never shows a mismatched split
     * while [selectedApps] was still catching up through [stateIn]/[flowOn].
     */
    private val _selectedApps = MutableStateFlow<List<DashboardAppItem>>(emptyList())
    private val _runningApps = MutableStateFlow<List<DashboardAppItem>>(emptyList())
    private val _stoppedApps = MutableStateFlow<List<DashboardAppItem>>(emptyList())

    val selectedApps: StateFlow<List<DashboardAppItem>> = _selectedApps.asStateFlow()
    val runningApps: StateFlow<List<DashboardAppItem>> = _runningApps.asStateFlow()
    val stoppedApps: StateFlow<List<DashboardAppItem>> = _stoppedApps.asStateFlow()

    init {
        viewModelScope.launch {
            var previous: Set<String>? = null
            selectedPackages.collect { current ->
                if (previous != null && current != previous && !_isIndexing.value) {
                    refreshAppStates()
                }
                previous = current
            }
        }
        viewModelScope.launch {
            appRuntimeStateRepository.stoppedPackages.collect { fromDataStore ->
                if (!_isIndexing.value) {
                    _uiStoppedPackages.value = fromDataStore
                    val items = _selectedApps.value
                    if (items.isNotEmpty()) {
                        applySplit(items, fromDataStore)
                    }
                }
            }
        }
        refreshAppStates()
    }

    private fun applySplit(items: List<DashboardAppItem>, stopped: Set<String>) {
        _runningApps.value = items.filterNot { it.packageName in stopped }
        _stoppedApps.value = items.filter { it.packageName in stopped }
    }

    fun clearSelection() {
        viewModelScope.launch(Dispatchers.Default) {
            repository.saveSelection(emptySet())
        }
    }

    fun removePackagesFromSelection(toRemove: Set<String>) {
        if (toRemove.isEmpty()) return
        viewModelScope.launch(Dispatchers.Default) {
            val next = selectedPackages.value - toRemove
            repository.saveSelection(next)
        }
    }

    fun refreshAppStates() {
        viewModelScope.launch {
            _isIndexing.value = true
            try {
                val application = getApplication<Application>()
                // stateIn starts as emptySet() until DataStore emits; reading the flow avoids a
                // false "empty dashboard" flash before the real selection loads.
                val selected = withContext(Dispatchers.IO) {
                    repository.selectedPackages.first()
                }
                val stopped = withContext(Dispatchers.IO) {
                    computeStoppedFromPackageManager(application, selected)
                }
                val items = withContext(Dispatchers.Default) {
                    loadDashboardItems(application, selected)
                }
                withContext(NonCancellable) {
                    withContext(Dispatchers.Main.immediate) {
                        _selectedApps.value = items
                        _uiStoppedPackages.value = stopped
                        applySplit(items, stopped)
                    }
                    withContext(Dispatchers.IO) {
                        runCatching {
                            appRuntimeStateRepository.setStoppedPackages(stopped)
                        }
                    }
                }
            } finally {
                withContext(Dispatchers.Main.immediate) {
                    _isIndexing.value = false
                }
            }
        }
    }
}

private fun computeStoppedFromPackageManager(
    application: Application,
    selected: Set<String>,
): Set<String> {
    val pm = application.packageManager
    val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES
    return selected.filterTo(mutableSetOf()) { packageName ->
        runCatching {
            val appInfo = pm.getApplicationInfo(packageName, flags)
            (appInfo.flags and ApplicationInfo.FLAG_STOPPED) != 0
        }.getOrDefault(false)
    }
}

private fun loadDashboardItems(
    application: Application,
    packageSet: Set<String>,
): List<DashboardAppItem> {
    if (packageSet.isEmpty()) return emptyList()
    val pm = application.packageManager
    val infoFlags = PackageManager.MATCH_UNINSTALLED_PACKAGES
    return packageSet.map { pkg ->
        runCatching {
            val appInfo = pm.getApplicationInfo(pkg, infoFlags)
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
            val isSystem = SystemAppClassifier.isSystem(appInfo)
            DashboardAppItem(
                packageName = pkg,
                label = label,
                icon = icon,
                isSystem = isSystem,
            )
        }.getOrElse {
            // Keep a row for every saved package so the dashboard matches DataStore even when
            // PackageManager lookup fails (visibility, OEM quirks). Selection uses a different
            // query path, so we'd otherwise show "no apps selected" while pickers still show checks.
            DashboardAppItem(
                packageName = pkg,
                label = pkg,
                icon = null,
                isSystem = false,
            )
        }
    }.sortedBy { it.label.lowercase() }
}
