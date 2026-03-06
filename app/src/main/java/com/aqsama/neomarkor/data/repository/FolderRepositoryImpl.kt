package com.aqsama.neomarkor.data.repository

import com.aqsama.neomarkor.data.local.StoragePreferences
import com.aqsama.neomarkor.domain.model.FolderMetadata
import com.aqsama.neomarkor.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FolderRepositoryImpl(
    private val prefs: StoragePreferences,
) : FolderRepository {

    override fun observeFolders(): Flow<List<FolderMetadata>> =
        prefs.observeFolderMetadataMap().map { map ->
            map.values.sortedWith(compareBy({ it.parentId }, { it.orderIndex }, { it.name }))
        }

    override fun observeNoteFolderMap(): Flow<Map<String, String>> =
        prefs.observeNoteFolderMap()

    override fun observeTrashedNotes(): Flow<Set<String>> =
        prefs.observeTrashedNotes()

    override suspend fun createFolder(
        name: String,
        colorArgb: Int,
        parentId: String?,
        uriString: String?,
    ): String {
        val existing = prefs.observeFolderMetadataMap().first()
        val siblings = existing.values.filter { it.parentId == parentId }
        val nextIndex = (siblings.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val metadata = FolderMetadata(
            name = name,
            colorArgb = colorArgb,
            parentId = parentId,
            orderIndex = nextIndex,
            uriString = uriString,
        )
        prefs.saveFolderMetadata(metadata)
        return metadata.id
    }

    override suspend fun updateFolder(
        id: String,
        name: String?,
        colorArgb: Int?,
    ) {
        val existing = prefs.observeFolderMetadataMap().first()
        val old = existing[id] ?: return
        val updated = old.copy(
            name = name ?: old.name,
            colorArgb = colorArgb ?: old.colorArgb,
        )
        prefs.saveFolderMetadata(updated)
    }

    override suspend fun moveFolderTo(id: String, newParentId: String?) {
        val existing = prefs.observeFolderMetadataMap().first()
        val old = existing[id] ?: return
        prefs.saveFolderMetadata(old.copy(parentId = newParentId))
    }

    override suspend fun reorderFolders(orderedIds: List<String>) {
        val existing = prefs.observeFolderMetadataMap().first().toMutableMap()
        orderedIds.forEachIndexed { index, id ->
            existing[id]?.let { existing[id] = it.copy(orderIndex = index) }
        }
        prefs.saveFolderMetadataMap(existing)
    }

    override suspend fun deleteFolder(id: String) {
        prefs.deleteFolderMetadata(id)
    }

    override suspend fun assignNoteToFolder(noteUri: String, folderId: String?) {
        prefs.assignNoteToFolder(noteUri, folderId)
    }

    override suspend fun trashNote(noteUri: String) {
        prefs.addToTrash(noteUri)
    }

    override suspend fun restoreNote(noteUri: String) {
        prefs.restoreFromTrash(noteUri)
    }

    override suspend fun emptyTrash() {
        prefs.emptyTrash()
    }
}
