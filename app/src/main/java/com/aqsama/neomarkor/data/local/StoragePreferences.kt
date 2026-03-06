package com.aqsama.neomarkor.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aqsama.neomarkor.domain.model.Folder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "neo_markor_prefs")

class StoragePreferences(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val rootDirUriKey = stringPreferencesKey("root_dir_uri")
    private val pinnedNotesKey = stringSetPreferencesKey("pinned_notes")
    private val themeModeKey = intPreferencesKey("theme_mode") // 0=system, 1=light, 2=dark
    private val accentColorKey = intPreferencesKey("accent_color") // ARGB int
    private val cornerRadiusKey = floatPreferencesKey("corner_radius") // dp
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val foldersKey = stringPreferencesKey("folders_json")
    private val trashedNotesKey = stringSetPreferencesKey("trashed_notes")

    // ── Root directory ──────────────────────────────────────────────────

    fun observeRootDirectoryUri(): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[rootDirUriKey] }

    suspend fun saveRootDirectoryUri(uriString: String) {
        context.dataStore.edit { prefs -> prefs[rootDirUriKey] = uriString }
    }

    // ── Pinned notes ────────────────────────────────────────────────────

    fun observePinnedNotes(): Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[pinnedNotesKey] ?: emptySet() }

    suspend fun togglePin(uriString: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[pinnedNotesKey] ?: emptySet()
            prefs[pinnedNotesKey] = if (uriString in current) current - uriString else current + uriString
        }
    }

    suspend fun isPinned(uriString: String): Boolean {
        val prefs = context.dataStore.data.first()
        return uriString in (prefs[pinnedNotesKey] ?: emptySet())
    }

    // ── Theme settings ──────────────────────────────────────────────────

    /** 0 = System, 1 = Light, 2 = Dark */
    fun observeThemeMode(): Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[themeModeKey] ?: 0 }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { prefs -> prefs[themeModeKey] = mode }
    }

    /** ARGB color int. Default = 0 means "use default palette". */
    fun observeAccentColor(): Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[accentColorKey] ?: 0 }

    suspend fun setAccentColor(color: Int) {
        context.dataStore.edit { prefs -> prefs[accentColorKey] = color }
    }

    fun observeCornerRadius(): Flow<Float> =
        context.dataStore.data.map { prefs -> prefs[cornerRadiusKey] ?: 12f }

    suspend fun setCornerRadius(radius: Float) {
        context.dataStore.edit { prefs -> prefs[cornerRadiusKey] = radius }
    }

    fun observeDynamicColor(): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[dynamicColorKey] ?: true }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[dynamicColorKey] = enabled }
    }

    // ── Folder management ───────────────────────────────────────────────

    fun observeFolders(): Flow<List<Folder>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[foldersKey]
            if (raw.isNullOrBlank()) emptyList()
            else try {
                json.decodeFromString<List<Folder>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
        }

    suspend fun saveFolders(folders: List<Folder>) {
        context.dataStore.edit { prefs ->
            prefs[foldersKey] = json.encodeToString(folders)
        }
    }

    suspend fun getFolders(): List<Folder> {
        val prefs = context.dataStore.data.first()
        val raw = prefs[foldersKey]
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<Folder>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Trash management ────────────────────────────────────────────────

    fun observeTrashedNotes(): Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[trashedNotesKey] ?: emptySet() }

    suspend fun moveToTrash(uriString: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[trashedNotesKey] ?: emptySet()
            prefs[trashedNotesKey] = current + uriString
        }
    }

    suspend fun restoreFromTrash(uriString: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[trashedNotesKey] ?: emptySet()
            prefs[trashedNotesKey] = current - uriString
        }
    }

    suspend fun clearTrash() {
        context.dataStore.edit { prefs ->
            prefs[trashedNotesKey] = emptySet()
        }
    }
}
