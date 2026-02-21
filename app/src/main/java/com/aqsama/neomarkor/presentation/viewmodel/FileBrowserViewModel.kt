package com.aqsama.neomarkor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.domain.repository.FileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FileBrowserViewModel(private val fileRepository: FileRepository) : ViewModel() {

    val fileTree: StateFlow<List<FileNode>> = fileRepository.observeFileTree()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val directoryUri: StateFlow<String?> = fileRepository.observeDirectoryUri()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDirectory(uriString: String) {
        viewModelScope.launch { fileRepository.setDirectoryUri(uriString) }
    }

    fun refresh() {
        viewModelScope.launch { fileRepository.refreshFileTree() }
    }
}
