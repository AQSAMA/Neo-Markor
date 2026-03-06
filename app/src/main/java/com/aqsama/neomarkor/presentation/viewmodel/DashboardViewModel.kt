package com.aqsama.neomarkor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqsama.neomarkor.data.local.StoragePreferences
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.domain.repository.FileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    private val fileRepository: FileRepository,
    private val storagePreferences: StoragePreferences,
) : ViewModel() {

    val fileTree: StateFlow<List<FileNode>> = fileRepository.observeFileTree()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Flat list of all leaf (non-directory) files sorted by last-modified descending, limited to 20. */
    val recentFiles: StateFlow<List<FileNode>> = fileRepository.observeFileTree()
        .map { tree ->
            flattenTree(tree)
                .sortedByDescending { it.lastModified }
                .take(20)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allNotes: StateFlow<List<FileNode>> = fileRepository.observeFileTree()
        .map { tree -> flattenTree(tree).sortedByDescending { it.lastModified } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasDirectory: StateFlow<Boolean> = fileRepository.observeDirectoryUri()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Set of pinned note URI strings. */
    val pinnedNoteUris: StateFlow<Set<String>> = storagePreferences.observePinnedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val folderColors: StateFlow<Map<String, Int>> = storagePreferences.observeFolderColors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val folders: StateFlow<List<FolderNodeUi>> = combine(
        fileRepository.observeFileTree(),
        folderColors,
    ) { tree, colors ->
        tree.filter { it.isDirectory }.map { directory ->
            buildFolderNode(directory, null, colors)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pinned notes as FileNode list (resolved from the tree). */
    val pinnedNotes: StateFlow<List<FileNode>> = combine(
        fileRepository.observeFileTree(),
        storagePreferences.observePinnedNotes(),
    ) { tree, pinned ->
        val allFiles = flattenTree(tree)
        allFiles.filter { it.uriString in pinned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Emits the URI of a freshly created note so the UI can navigate to it. */
    private val _newNoteEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val newNoteEvent: SharedFlow<String> = _newNoteEvent.asSharedFlow()

    fun createNewNote() {
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "Note $timestamp.md"
            val uri = fileRepository.createFile(
                fileName = fileName,
                initialContent = "# Untitled Note\n\n",
            )
            if (uri != null) {
                _newNoteEvent.emit(uri)
            }
        }
    }

    /** Creates or opens today's daily note (YYYY-MM-DD.md). */
    fun openDailyNote() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dailyFileName = "$today.md"
            // Search the full file tree for an existing daily note
            val allFiles = flattenTree(fileRepository.observeFileTree().first())
            val existing = allFiles.find {
                it.name.equals(dailyFileName, ignoreCase = true)
            }
            if (existing != null) {
                _newNoteEvent.emit(existing.uriString)
            } else {
                val uri = fileRepository.createFile(
                    fileName = dailyFileName,
                    initialContent = "# Daily Note — $today\n\n",
                )
                if (uri != null) _newNoteEvent.emit(uri)
            }
        }
    }

    fun togglePin(uriString: String) {
        viewModelScope.launch { storagePreferences.togglePin(uriString) }
    }

    fun createFolder(name: String, parentUri: String?, color: Int?) {
        viewModelScope.launch {
            val uri = fileRepository.createFolder(folderName = name, parentUriString = parentUri)
            if (uri != null && color != null) {
                storagePreferences.setFolderColor(uri, color)
            }
        }
    }

    fun renameFolder(uri: String, newName: String) {
        viewModelScope.launch {
            val renamedUri = fileRepository.renameFile(uri, newName)
            if (renamedUri != null && renamedUri != uri) {
                val colors = folderColors.value
                val existing = colors[uri]
                if (existing != null) {
                    storagePreferences.removeFolderColor(uri)
                    storagePreferences.setFolderColor(renamedUri, existing)
                }
            }
        }
    }

    fun deleteFolders(uris: Set<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uris.forEach { fileRepository.deleteFile(it) }
            storagePreferences.removeFolderColors(uris)
        }
    }

    fun moveFolders(uris: Set<String>, targetParentUri: String?) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val destination = targetParentUri ?: fileRepository.observeDirectoryUri().first() ?: return@launch
            uris.filter { it != destination }.forEach { source ->
                fileRepository.moveFile(source, destination)
            }
        }
    }

    fun setFolderColor(uris: Set<String>, color: Int) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uris.forEach { storagePreferences.setFolderColor(it, color) }
        }
    }

    private fun flattenTree(nodes: List<FileNode>): List<FileNode> =
        nodes.flatMap { node ->
            if (node.isDirectory) flattenTree(node.children) else listOf(node)
        }

    private fun buildFolderNode(
        directory: FileNode,
        parentUri: String?,
        colors: Map<String, Int>,
    ): FolderNodeUi {
        val childFolders = directory.children.filter { it.isDirectory }
            .map { buildFolderNode(it, directory.uriString, colors) }
        val noteCount = countDescendantNotes(directory)
        return FolderNodeUi(
            uri = directory.uriString,
            parentUri = parentUri,
            name = directory.name,
            noteCount = noteCount,
            colorArgb = colors[directory.uriString],
            children = childFolders,
        )
    }

    private fun countDescendantNotes(node: FileNode): Int =
        node.children.sumOf { child ->
            if (child.isDirectory) countDescendantNotes(child) else 1
        }
}

data class FolderNodeUi(
    val uri: String,
    val parentUri: String?,
    val name: String,
    val noteCount: Int,
    val colorArgb: Int?,
    val children: List<FolderNodeUi> = emptyList(),
)
