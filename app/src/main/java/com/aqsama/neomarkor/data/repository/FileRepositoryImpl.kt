package com.aqsama.neomarkor.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.aqsama.neomarkor.data.local.StoragePreferences
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.domain.repository.FileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.buffer
import okio.source

private val SUPPORTED_EXTENSIONS = setOf("md", "txt")

class FileRepositoryImpl(
    private val context: Context,
    private val storagePreferences: StoragePreferences,
    private val scope: CoroutineScope,
) : FileRepository {
    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())

    init {
        scope.launch {
            storagePreferences.observeRootDirectoryUri().collect { uriString ->
                _fileTree.value = if (uriString != null) scanDocumentTree(uriString) else emptyList()
            }
        }
    }

    override fun observeFileTree(): Flow<List<FileNode>> = _fileTree.asStateFlow()

    override fun observeDirectoryUri(): Flow<String?> =
        storagePreferences.observeRootDirectoryUri()

    override suspend fun setDirectoryUri(uriString: String) {
        val uri = Uri.parse(uriString)
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        storagePreferences.saveRootDirectoryUri(uriString)
    }

    override suspend fun refreshFileTree() {
        val uriString = storagePreferences.observeRootDirectoryUri().first()
        if (uriString != null) {
            _fileTree.value = scanDocumentTree(uriString)
        }
    }

    override suspend fun readFile(uriString: String): String {
        val uri = Uri.parse(uriString)
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.source().buffer().readUtf8()
        } ?: ""
    }

    private fun scanDocumentTree(uriString: String): List<FileNode> {
        val uri = Uri.parse(uriString)
        val rootDocument = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
        return buildChildren(rootDocument)
    }

    private fun buildChildren(directory: DocumentFile): List<FileNode> =
        directory.listFiles()
            .mapNotNull { doc ->
                val name = doc.name ?: return@mapNotNull null
                when {
                    doc.isDirectory -> FileNode(
                        name = name,
                        uriString = doc.uri.toString(),
                        isDirectory = true,
                        children = buildChildren(doc),
                    )
                    doc.isFile && name.contains('.') && name.substringAfterLast('.').lowercase() in SUPPORTED_EXTENSIONS ->
                        FileNode(
                            name = name,
                            uriString = doc.uri.toString(),
                            isDirectory = false,
                        )
                    else -> null
                }
            }
            .sortedWith(compareByDescending<FileNode> { it.isDirectory }.thenBy { it.name })
}
