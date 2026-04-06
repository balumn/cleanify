package com.cleanify.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the set of package names the user has selected for cleanup.
 *
 * DataStore is used (instead of Room) because this app only needs a single small piece of state:
 * a set of strings.
 */
class SelectedAppsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private val selectedPackagesKey = stringSetPreferencesKey("selected_packages")

    /**
     * A reactive stream of the selected package names.
     */
    val selectedPackages: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[selectedPackagesKey].orEmpty()
    }

    /**
     * Persists the given selection.
     */
    suspend fun saveSelection(packages: Set<String>) {
        // Basic hygiene: avoid persisting empty/blank entries.
        val sanitized = packages
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSortedSet()

        dataStore.edit { prefs ->
            prefs[selectedPackagesKey] = sanitized
        }
    }
}

