package com.cleanify.feature.selection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanify.core.InstalledAppsProvider
import com.cleanify.data.SelectedAppsRepository
import com.cleanify.data.cleanifyDataStore
import com.cleanify.data.model.AppListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AppSelectionLoadState {
    data object Loading : AppSelectionLoadState
    data class Error(val message: String) : AppSelectionLoadState
    data class Loaded(val appsCount: Int) : AppSelectionLoadState
}

class AppSelectionViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = SelectedAppsRepository(application.cleanifyDataStore)
    private val installedAppsProvider = InstalledAppsProvider(application)

    private val _apps = MutableStateFlow<List<AppListItem>>(emptyList())
    val apps: StateFlow<List<AppListItem>> = _apps.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    private val _loadState = MutableStateFlow<AppSelectionLoadState>(AppSelectionLoadState.Loading)
    val loadState: StateFlow<AppSelectionLoadState> = _loadState.asStateFlow()

    init {
        viewModelScope.launch {
            // Keep selection state reactive to DataStore.
            launch {
                repository.selectedPackages.collect { selected ->
                    _selectedPackages.value = selected
                }
            }

            // Load installed apps once on first entry.
            val result = installedAppsProvider.loadAllInstalledApps()
            result.onSuccess { items ->
                _apps.value = items
                _loadState.value = AppSelectionLoadState.Loaded(items.size)
            }.onFailure { t ->
                val message = when (t) {
                    is SecurityException -> {
                        "Your device restricted package visibility. Cleanify needs `android.permission.QUERY_ALL_PACKAGES` in its manifest. Reinstall the app with that permission granted at install time."
                    }
                    else -> t.message ?: "Unable to load installed apps."
                }
                _loadState.value = AppSelectionLoadState.Error(
                    message,
                )
            }
        }
    }

    fun togglePackage(packageName: String, enabled: Boolean) {
        val current = _selectedPackages.value
        _selectedPackages.value = if (enabled) {
            current + packageName
        } else {
            current - packageName
        }
    }

    fun selectAll() {
        _selectedPackages.value = _apps.value.map { it.packageName }.toSet()
    }

    fun deselectAll() {
        _selectedPackages.value = emptySet()
    }

    suspend fun saveSelection() {
        // Ensure we persist only trimmed non-empty values.
        val sanitized = _selectedPackages.value.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        repository.saveSelection(sanitized)
    }

    /**
     * Convenience for the UI: saves then returns.
     */
    fun saveAndReturn(onSaved: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    saveSelection()
                }
                onSaved()
            } catch (t: Throwable) {
                onError(t.message ?: "Failed to save selection.")
            }
        }
    }
}

