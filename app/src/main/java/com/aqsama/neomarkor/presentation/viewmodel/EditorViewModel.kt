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

    // ── Undo / Redo ─────────────────────────────────────────────────────

    private val undoStack = ArrayDeque<String>(MAX_UNDO_STACK)
    private val redoStack = ArrayDeque<String>(MAX_UNDO_STACK)

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // ── Export ───────────────────────────────────────────────────────────

    private val _exportHtml = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val exportHtml: MutableSharedFlow<String> = _exportHtml

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
                try {
                    _content.value = fileRepository.readFile(fileUriString)
                } catch (e: Exception) {
                    _content.value = "# Error opening file\n\nThe file could not be loaded. Please go back and try again."
                    android.util.Log.e("EditorViewModel", "readFile failed for $fileUriString", e)
                } finally {
                    _isLoading.value = false
                }
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
        // Push current state to undo stack before changing
        val old = _content.value
        if (old != newContent) {
            pushUndo(old)
            redoStack.clear()
            _canRedo.value = false
        }
        _content.value = newContent
        if (!isNewNote) _saveFlow.tryEmit(newContent)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(_content.value)
        _content.value = undoStack.removeLast()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
        if (!isNewNote) _saveFlow.tryEmit(_content.value)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(_content.value)
        _content.value = redoStack.removeLast()
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()
        if (!isNewNote) _saveFlow.tryEmit(_content.value)
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

    /** Generates full HTML for the current content and emits it via [exportHtml]. */
    fun requestExportHtml() {
        val html = fileRepository.markdownToHtml(_content.value)
        _exportHtml.tryEmit(html)
    }

    /** Returns the file extension (lowercase) for format-aware editor behavior. */
    fun getFileExtension(): String {
        val name = _fileName.value
        return if (name.contains('.')) name.substringAfterLast('.').lowercase() else "md"
    }

    private fun pushUndo(text: String) {
        if (undoStack.size >= MAX_UNDO_STACK) undoStack.removeFirst()
        undoStack.addLast(text)
        _canUndo.value = true
    }

    private fun extractFileName(uriString: String): String = when {
        uriString.startsWith("content://") ->
            Uri.parse(uriString).lastPathSegment
                ?.substringAfterLast("/")
                ?.substringAfterLast(":")
                ?: "File"
        else -> uriString.substringAfterLast("/")
    }

    companion object {
        private const val MAX_UNDO_STACK = 50
    }
}
