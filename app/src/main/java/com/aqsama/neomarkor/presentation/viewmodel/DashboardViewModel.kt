package com.aqsama.neomarkor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.domain.repository.FileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(private val fileRepository: FileRepository) : ViewModel() {

    /** Flat list of all leaf (non-directory) files in the tree, limited to 20 for the recent view. */
    val recentFiles: StateFlow<List<FileNode>> = fileRepository.observeFileTree()
        .map { tree -> flattenTree(tree).take(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasDirectory: StateFlow<Boolean> = fileRepository.observeDirectoryUri()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    private fun flattenTree(nodes: List<FileNode>): List<FileNode> =
        nodes.flatMap { node ->
            if (node.isDirectory) flattenTree(node.children) else listOf(node)
        }
}
