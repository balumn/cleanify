package com.cleanify.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore

private const val DATASTORE_NAME = "cleanify_prefs"

val Context.cleanifyDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

