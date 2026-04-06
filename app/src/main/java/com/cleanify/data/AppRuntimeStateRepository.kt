package com.cleanify.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRuntimeStateRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private val stoppedPackagesKey = stringSetPreferencesKey("stopped_packages")

    val stoppedPackages: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[stoppedPackagesKey].orEmpty()
    }

    suspend fun markStopped(packageName: String) {
        val sanitized = packageName.trim()
        if (sanitized.isEmpty()) return

        dataStore.edit { prefs ->
            val current = prefs[stoppedPackagesKey].orEmpty()
            prefs[stoppedPackagesKey] = current + sanitized
        }
    }

    suspend fun retainOnlySelected(selectedPackages: Set<String>) {
        dataStore.edit { prefs ->
            val selected = selectedPackages.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            val current = prefs[stoppedPackagesKey].orEmpty()
            prefs[stoppedPackagesKey] = current.filterTo(mutableSetOf()) { it in selected }
        }
    }

    suspend fun setStoppedPackages(stoppedPackages: Set<String>) {
        dataStore.edit { prefs ->
            prefs[stoppedPackagesKey] = stoppedPackages
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
    }
}
