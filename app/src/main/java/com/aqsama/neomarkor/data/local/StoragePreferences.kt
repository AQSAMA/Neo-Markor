package com.aqsama.neomarkor.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aqsama.neomarkor.domain.model.FolderMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "neo_markor_prefs")

class StoragePreferences(private val context: Context) {

    private val rootDirUriKey = stringPreferencesKey("root_dir_uri")
    private val pinnedNotesKey = stringSetPreferencesKey("pinned_notes")
    private val themeModeKey = intPreferencesKey("theme_mode") // 0=system, 1=light, 2=dark
    private val accentColorKey = intPreferencesKey("accent_color") // ARGB int
    private val cornerRadiusKey = floatPreferencesKey("corner_radius") // dp
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    // ── Folder metadata ─────────────────────────────────────────────────
    private val folderMetadataJsonKey = stringPreferencesKey("folder_metadata_json")
    // Note → folder mapping: JSON of Map<noteUri, folderId>
    private val noteFolderMapJsonKey = stringPreferencesKey("note_folder_map_json")
    // Trashed note URIs
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

    // ── Folder metadata ──────────────────────────────────────────────────

    /** Emits the full map of folderId → FolderMetadata. */
    fun observeFolderMetadataMap(): Flow<Map<String, FolderMetadata>> =
        context.dataStore.data.map { prefs ->
            val json = prefs[folderMetadataJsonKey] ?: return@map emptyMap()
            try {
                Json.decodeFromString<Map<String, FolderMetadata>>(json)
            } catch (e: Exception) {
                Log.w("StoragePreferences", "Failed to decode folder metadata", e)
                emptyMap()
            }
        }

    /** Saves or replaces the metadata for a single folder. */
    suspend fun saveFolderMetadata(metadata: FolderMetadata) {
        context.dataStore.edit { prefs ->
            val current: Map<String, FolderMetadata> = try {
                Json.decodeFromString(prefs[folderMetadataJsonKey] ?: "{}")
            } catch (e: Exception) {
                Log.w("StoragePreferences", "Failed to decode folder metadata map, resetting", e)
                emptyMap()
            }
            val updated = current + (metadata.id to metadata)
            prefs[folderMetadataJsonKey] = Json.encodeToString(updated)
        }
    }

    /** Removes the metadata for a single folder (and its children transitively). */
    suspend fun deleteFolderMetadata(id: String) {
        context.dataStore.edit { prefs ->
            val current: Map<String, FolderMetadata> = try {
                Json.decodeFromString(prefs[folderMetadataJsonKey] ?: "{}")
            } catch (e: Exception) {
                Log.w("StoragePreferences", "Failed to decode folder metadata map on delete", e)
                emptyMap()
            }
            // Remove this folder and any descendants
            val toRemove = mutableSetOf(id)
            var changed = true
            while (changed) {
                changed = false
                current.values.forEach { f ->
                    if (f.parentId in toRemove && f.id !in toRemove) {
                        toRemove += f.id
                        changed = true
                    }
                }
            }
            val updated = current.filterKeys { it !in toRemove }
            prefs[folderMetadataJsonKey] = Json.encodeToString(updated)
        }
    }

    /** Replaces the full folder map (used after bulk reorder). */
    suspend fun saveFolderMetadataMap(map: Map<String, FolderMetadata>) {
        context.dataStore.edit { prefs ->
            prefs[folderMetadataJsonKey] = Json.encodeToString(map)
        }
    }

    // ── Note → Folder mapping ────────────────────────────────────────────

    fun observeNoteFolderMap(): Flow<Map<String, String>> =
        context.dataStore.data.map { prefs ->
            val json = prefs[noteFolderMapJsonKey] ?: return@map emptyMap()
            try {
                Json.decodeFromString<Map<String, String>>(json)
            } catch (e: Exception) {
                Log.w("StoragePreferences", "Failed to decode note-folder map", e)
                emptyMap()
            }
        }

    suspend fun assignNoteToFolder(noteUri: String, folderId: String?) {
        context.dataStore.edit { prefs ->
            val current: Map<String, String> = try {
                Json.decodeFromString(prefs[noteFolderMapJsonKey] ?: "{}")
            } catch (e: Exception) {
                Log.w("StoragePreferences", "Failed to decode note-folder map on assign", e)
                emptyMap()
            }
            val updated = if (folderId == null) current - noteUri else current + (noteUri to folderId)
            prefs[noteFolderMapJsonKey] = Json.encodeToString(updated)
        }
    }

    // ── Trash ────────────────────────────────────────────────────────────

    fun observeTrashedNotes(): Flow<Set<String>> =
        context.dataStore.data.map { prefs -> prefs[trashedNotesKey] ?: emptySet() }

    suspend fun addToTrash(uriString: String) {
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

    suspend fun emptyTrash() {
        context.dataStore.edit { prefs -> prefs[trashedNotesKey] = emptySet() }
    }
}
