package com.aqsama.neomarkor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.domain.repository.FileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(fileRepository: FileRepository) : ViewModel() {

    /** Flat list of all leaf (non-directory) files in the tree, limited to 20 for the recent view. */
    val recentFiles: StateFlow<List<FileNode>> = fileRepository.observeFileTree()
        .map { tree -> flattenTree(tree).take(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasDirectory: StateFlow<Boolean> = fileRepository.observeDirectoryUri()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private fun flattenTree(nodes: List<FileNode>): List<FileNode> =
        nodes.flatMap { node ->
            if (node.isDirectory) flattenTree(node.children) else listOf(node)
        }
}
