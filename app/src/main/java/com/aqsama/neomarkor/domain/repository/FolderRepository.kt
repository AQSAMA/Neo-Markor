package com.aqsama.neomarkor.domain.repository

import com.aqsama.neomarkor.domain.model.FolderMetadata
import kotlinx.coroutines.flow.Flow

interface FolderRepository {

    /** Emits all virtual folders, ordered by (parentId, orderIndex). */
    fun observeFolders(): Flow<List<FolderMetadata>>

    /** Emits the note-URI → folder-id mapping. */
    fun observeNoteFolderMap(): Flow<Map<String, String>>

    /** Emits the set of trashed note URIs. */
    fun observeTrashedNotes(): Flow<Set<String>>

    /**
     * Creates a new folder with the given name and color, under [parentId] (null = root).
     * Returns the new folder's id.
     */
    suspend fun createFolder(name: String, colorArgb: Int, parentId: String? = null): String

    /** Updates an existing folder's name and/or color. Null parameters are left unchanged. */
    suspend fun updateFolder(
        id: String,
        name: String? = null,
        colorArgb: Int? = null,
    )

    /** Moves a folder to a new parent (null = root). */
    suspend fun moveFolderTo(id: String, newParentId: String?)

    /** Reorders the given list of folder ids by assigning sequential orderIndex values. */
    suspend fun reorderFolders(orderedIds: List<String>)

    /** Deletes a folder (and removes its children transitively) from the metadata store. */
    suspend fun deleteFolder(id: String)

    /** Assigns a note to a folder, or removes it from any folder if [folderId] is null. */
    suspend fun assignNoteToFolder(noteUri: String, folderId: String?)

    /** Moves a note to the trash. */
    suspend fun trashNote(noteUri: String)

    /** Restores a note from the trash. */
    suspend fun restoreNote(noteUri: String)

    /** Permanently removes all notes from the trash (caller is responsible for SAF deletion). */
    suspend fun emptyTrash()
}
