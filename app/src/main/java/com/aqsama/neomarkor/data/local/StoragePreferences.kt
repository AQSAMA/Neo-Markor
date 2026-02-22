package com.aqsama.neomarkor.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "neo_markor_prefs")

class StoragePreferences(private val context: Context) {

    private val rootDirUriKey = stringPreferencesKey("root_dir_uri")

    fun observeRootDirectoryUri(): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[rootDirUriKey] }

    suspend fun saveRootDirectoryUri(uriString: String) {
        context.dataStore.edit { prefs -> prefs[rootDirUriKey] = uriString }
    }
}
