package com.aqsama.neomarkor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqsama.neomarkor.data.local.StoragePreferences
import com.aqsama.neomarkor.domain.model.Folder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class FolderViewModel(
    private val storagePreferences: StoragePreferences,
) : ViewModel() {

    val folders: StateFlow<List<Folder>> = storagePreferences.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedFolderIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolderIds: StateFlow<Set<String>> = _selectedFolderIds.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
        if (!_isEditMode.value) {
            _selectedFolderIds.value = emptySet()
        }
    }

    fun exitEditMode() {
        _isEditMode.value = false
        _selectedFolderIds.value = emptySet()
    }

    fun toggleSelection(folderId: String) {
        val current = _selectedFolderIds.value
        _selectedFolderIds.value = if (folderId in current) current - folderId else current + folderId
    }

    fun selectAll() {
        _selectedFolderIds.value = folders.value.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedFolderIds.value = emptySet()
    }

    fun createFolder(name: String, color: Long, parentId: String? = null) {
        viewModelScope.launch {
            val current = storagePreferences.getFolders()
            val maxOrder = current.filter { it.parentId == parentId }.maxOfOrNull { it.order } ?: -1
            val newFolder = Folder(
                id = UUID.randomUUID().toString(),
                name = name,
                color = color,
                parentId = parentId,
                order = maxOrder + 1,
            )
            storagePreferences.saveFolders(current + newFolder)
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            val current = storagePreferences.getFolders()
            val updated = current.map {
                if (it.id == folderId) it.copy(name = newName) else it
            }
            storagePreferences.saveFolders(updated)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            val current = storagePreferences.getFolders()
            // Delete the folder and all its descendants
            val idsToDelete = collectDescendantIds(folderId, current) + folderId
            val updated = current.filter { it.id !in idsToDelete }
            storagePreferences.saveFolders(updated)
        }
    }

    fun deleteFolders(folderIds: Set<String>) {
        viewModelScope.launch {
            val current = storagePreferences.getFolders()
            val allIdsToDelete = mutableSetOf<String>()
            for (id in folderIds) {
                allIdsToDelete.add(id)
                allIdsToDelete.addAll(collectDescendantIds(id, current))
            }
            val updated = current.filter { it.id !in allIdsToDelete }
            storagePreferences.saveFolders(updated)
            _selectedFolderIds.value = emptySet()
        }
    }

    fun setFolderColor(folderId: String, color: Long) {
        viewModelScope.launch {
            val current = storagePreferences.getFolders()
            val updated = current.map {
                if (it.id == folderId) it.copy(color = color) else it
            }
            storagePreferences.saveFolders(updated)
        }
    }

    fun setFoldersColor(folderIds: Set<String>, color: Long) {
        viewModelScope.launch {
            val current = storagePreferences.getFolders()
            val updated = current.map {
                if (it.id in folderIds) it.copy(color = color) else it
            }
            storagePreferences.saveFolders(updated)
        }
    }

    fun moveFolder(folderId: String, newParentId: String?) {
        viewModelScope.launch {
            val current = storagePreferences.getFolders()
            // Prevent circular reference: don't move to own descendants
            if (newParentId != null) {
                val descendants = collectDescendantIds(folderId, current)
                if (newParentId in descendants || newParentId == folderId) return@launch
            }
            val siblings = current.filter { it.parentId == newParentId }
            val maxOrder = siblings.maxOfOrNull { it.order } ?: -1
            val updated = current.map {
                if (it.id == folderId) it.copy(parentId = newParentId, order = maxOrder + 1) else it
            }
            storagePreferences.saveFolders(updated)
        }
    }

    fun moveFolders(folderIds: Set<String>, newParentId: String?) {
        viewModelScope.launch {
            val current = storagePreferences.getFolders()
            // Prevent circular reference: don't move into own descendants
            if (newParentId != null) {
                for (id in folderIds) {
                    val descendants = collectDescendantIds(id, current)
                    if (newParentId in descendants || newParentId == id) return@launch
                }
            }
            val siblings = current.filter { it.parentId == newParentId }
            var nextOrder = (siblings.maxOfOrNull { it.order } ?: -1) + 1
            val updated = current.map {
                if (it.id in folderIds) {
                    val folder = it.copy(parentId = newParentId, order = nextOrder)
                    nextOrder++
                    folder
                } else it
            }
            storagePreferences.saveFolders(updated)
            _selectedFolderIds.value = emptySet()
        }
    }

    fun reorderFolder(folderId: String, newOrder: Int) {
        viewModelScope.launch {
            val current = storagePreferences.getFolders()
            val folder = current.find { it.id == folderId } ?: return@launch
            val siblings = current.filter { it.parentId == folder.parentId && it.id != folderId }
                .sortedBy { it.order }
            val totalCount = siblings.size + 1 // siblings + the moved folder
            val clampedOrder = newOrder.coerceIn(0, siblings.size)
            val reordered = mutableListOf<Folder>()
            var index = 0
            for (i in 0 until totalCount) {
                if (i == clampedOrder) {
                    reordered.add(folder.copy(order = i))
                } else if (index < siblings.size) {
                    reordered.add(siblings[index].copy(order = i))
                    index++
                }
            }
            val reorderedIds = reordered.associate { it.id to it.order }
            val updated = current.map { f ->
                reorderedIds[f.id]?.let { order -> f.copy(order = order) } ?: f
            }
            storagePreferences.saveFolders(updated)
        }
    }

    /** Get root folders (no parent) sorted by order. */
    fun getRootFolders(allFolders: List<Folder>): List<Folder> =
        allFolders.filter { it.parentId == null }.sortedBy { it.order }

    /** Get child folders of a given parent sorted by order. */
    fun getChildFolders(parentId: String, allFolders: List<Folder>): List<Folder> =
        allFolders.filter { it.parentId == parentId }.sortedBy { it.order }

    /** Check if a folder has children. */
    fun hasChildren(folderId: String, allFolders: List<Folder>): Boolean =
        allFolders.any { it.parentId == folderId }

    /** Count total notes recursively in a folder subtree. */
    fun countNotesInSubtree(folderId: String, allFolders: List<Folder>): Int {
        val folder = allFolders.find { it.id == folderId } ?: return 0
        val childCount = allFolders
            .filter { it.parentId == folderId }
            .sumOf { countNotesInSubtree(it.id, allFolders) }
        return folder.noteCount + childCount
    }

    private fun collectDescendantIds(parentId: String, allFolders: List<Folder>): Set<String> {
        val children = allFolders.filter { it.parentId == parentId }
        val result = mutableSetOf<String>()
        for (child in children) {
            result.add(child.id)
            result.addAll(collectDescendantIds(child.id, allFolders))
        }
        return result
    }
}
