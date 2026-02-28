package com.aqsama.neomarkor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqsama.neomarkor.domain.model.FolderMetadata
import com.aqsama.neomarkor.domain.repository.FolderRepository
import com.aqsama.neomarkor.domain.repository.FileRepository
import com.aqsama.neomarkor.domain.model.FileNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FolderUiState(
    val folders: List<FolderMetadata> = emptyList(),
    val expandedIds: Set<String> = emptySet(),
    val isEditMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val noteFolderMap: Map<String, String> = emptyMap(),
    val allNoteUris: List<String> = emptyList(),
    val trashedNoteUris: Set<String> = emptySet(),
)

class FolderViewModel(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderUiState())
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    /** Emits all folders with note-counts, used by the drawer. */
    val foldersWithCounts: StateFlow<List<Pair<FolderMetadata, Int>>> =
        combine(
            folderRepository.observeFolders(),
            folderRepository.observeNoteFolderMap(),
            fileRepository.observeFileTree(),
            folderRepository.observeTrashedNotes(),
        ) { folders, noteFolderMap, tree, trashed ->
            val allNoteNodes = flattenNotes(tree).filter { it.uriString !in trashed }
            folders.map { folder ->
                val count = allNoteNodes.count { note ->
                    noteFolderMap[note.uriString] == folder.id
                }
                folder to count
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Flat list of all non-trashed notes (for "All notes" view). */
    val allNotes: StateFlow<List<FileNode>> =
        combine(
            fileRepository.observeFileTree(),
            folderRepository.observeTrashedNotes(),
        ) { tree, trashed ->
            flattenNotes(tree).filter { it.uriString !in trashed }
                .sortedByDescending { it.lastModified }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Emits notes currently in trash. */
    val trashedNotes: StateFlow<List<FileNode>> =
        combine(
            fileRepository.observeFileTree(),
            folderRepository.observeTrashedNotes(),
        ) { tree, trashed ->
            flattenNotes(tree).filter { it.uriString in trashed }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(
                folderRepository.observeFolders(),
                folderRepository.observeNoteFolderMap(),
                fileRepository.observeFileTree(),
                folderRepository.observeTrashedNotes(),
            ) { folders, noteFolderMap, tree, trashed ->
                FolderUiState(
                    folders = folders,
                    expandedIds = _uiState.value.expandedIds,
                    isEditMode = _uiState.value.isEditMode,
                    selectedIds = _uiState.value.selectedIds,
                    noteFolderMap = noteFolderMap,
                    allNoteUris = flattenNotes(tree).map { it.uriString },
                    trashedNoteUris = trashed,
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun toggleExpanded(id: String) {
        _uiState.update { s ->
            val expanded = if (id in s.expandedIds) s.expandedIds - id else s.expandedIds + id
            s.copy(expandedIds = expanded)
        }
    }

    fun enterEditMode() = _uiState.update { it.copy(isEditMode = true, selectedIds = emptySet()) }
    fun exitEditMode() = _uiState.update { it.copy(isEditMode = false, selectedIds = emptySet()) }

    fun toggleSelected(id: String) {
        _uiState.update { s ->
            val selected = if (id in s.selectedIds) s.selectedIds - id else s.selectedIds + id
            s.copy(selectedIds = selected)
        }
    }

    fun selectAll() {
        _uiState.update { s -> s.copy(selectedIds = s.folders.map { it.id }.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { s -> s.copy(selectedIds = emptySet()) }
    }

    fun createFolder(name: String, colorArgb: Int, parentId: String? = null) {
        viewModelScope.launch {
            folderRepository.createFolder(name, colorArgb, parentId)
        }
    }

    fun renameFolder(id: String, newName: String) {
        viewModelScope.launch { folderRepository.updateFolder(id, name = newName) }
    }

    fun setFolderColor(id: String, colorArgb: Int) {
        viewModelScope.launch { folderRepository.updateFolder(id, colorArgb = colorArgb) }
    }

    fun moveFolder(id: String, newParentId: String?) {
        viewModelScope.launch { folderRepository.moveFolderTo(id, newParentId) }
    }

    fun deleteFolders(ids: Set<String>) {
        viewModelScope.launch {
            ids.forEach { folderRepository.deleteFolder(it) }
            exitEditMode()
        }
    }

    fun reorderFolders(orderedIds: List<String>) {
        viewModelScope.launch { folderRepository.reorderFolders(orderedIds) }
    }

    fun assignNoteToFolder(noteUri: String, folderId: String?) {
        viewModelScope.launch { folderRepository.assignNoteToFolder(noteUri, folderId) }
    }

    fun trashNote(noteUri: String) {
        viewModelScope.launch { folderRepository.trashNote(noteUri) }
    }

    fun restoreNote(noteUri: String) {
        viewModelScope.launch { folderRepository.restoreNote(noteUri) }
    }

    fun emptyTrash() {
        viewModelScope.launch { folderRepository.emptyTrash() }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Returns a flat list of folders visible in the management screen (respecting expand state). */
    fun visibleFlatFolders(
        folders: List<FolderMetadata>,
        expandedIds: Set<String>,
        parentId: String? = null,
        depth: Int = 0,
    ): List<Pair<FolderMetadata, Int>> {
        val children = folders.filter { it.parentId == parentId }.sortedWith(
            compareBy({ it.orderIndex }, { it.name })
        )
        return children.flatMap { folder ->
            val entry = listOf(folder to depth)
            val hasChildren = folders.any { it.parentId == folder.id }
            if (hasChildren && folder.id in expandedIds) {
                entry + visibleFlatFolders(folders, expandedIds, folder.id, depth + 1)
            } else {
                entry
            }
        }
    }

    private fun flattenNotes(nodes: List<FileNode>): List<FileNode> =
        nodes.flatMap { n -> if (n.isDirectory) flattenNotes(n.children) else listOf(n) }
}
