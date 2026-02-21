package com.aqsama.neomarkor.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqsama.neomarkor.domain.repository.FileRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class EditorViewModel(
    private val fileUriString: String,
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _fileName = MutableStateFlow("File")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Only emitted by [onContentChanged]; drives debounced autosave. Overflow drops oldest so we always process the latest text. */
    private val _saveFlow = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val isNewNote = fileUriString == "new_note"

    init {
        _fileName.value = if (isNewNote) "New Note" else extractFileName(fileUriString)

        if (!isNewNote) {
            viewModelScope.launch {
                _content.value = fileRepository.readFile(fileUriString)
                _isLoading.value = false
            }
            viewModelScope.launch {
                _saveFlow
                    .debounce(1_000L)
                    .collect { text ->
                        _isSaving.value = true
                        try {
                            fileRepository.writeFile(fileUriString, text)
                        } finally {
                            _isSaving.value = false
                        }
                    }
            }
        } else {
            _isLoading.value = false
        }
    }

    fun onContentChanged(newContent: String) {
        _content.value = newContent
        if (!isNewNote) _saveFlow.tryEmit(newContent)
    }

    fun saveNow() {
        if (isNewNote) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                fileRepository.writeFile(fileUriString, _content.value)
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun extractFileName(uriString: String): String = when {
        uriString.startsWith("content://") ->
            Uri.parse(uriString).lastPathSegment
                ?.substringAfterLast("/")
                ?.substringAfterLast(":")
                ?: "File"
        else -> uriString.substringAfterLast("/")
    }
}
