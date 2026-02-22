package com.aqsama.neomarkor.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.aqsama.neomarkor.data.local.StoragePreferences
import com.aqsama.neomarkor.domain.model.FileNode
import com.aqsama.neomarkor.domain.repository.FileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
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

    override suspend fun readFile(uriString: String): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.source().buffer().readUtf8()
        } ?: ""
    }

    override suspend fun writeFile(uriString: String, content: String): Unit = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        // "wt" = write + truncate so existing content is fully replaced
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.sink().buffer().use { it.writeUtf8(content) }
        }
        Unit
    }

    override suspend fun createFile(fileName: String, initialContent: String): String? = withContext(Dispatchers.IO) {
        val rootUriString = storagePreferences.observeRootDirectoryUri().first() ?: return@withContext null
        val rootDoc = DocumentFile.fromTreeUri(context, Uri.parse(rootUriString)) ?: return@withContext null
        val mimeType = if (fileName.endsWith(".md")) "text/markdown" else "text/plain"
        val newDoc = rootDoc.createFile(mimeType, fileName) ?: return@withContext null
        val newUriString = newDoc.uri.toString()
        if (initialContent.isNotEmpty()) {
            context.contentResolver.openOutputStream(newDoc.uri, "wt")?.use { stream ->
                stream.sink().buffer().use { it.writeUtf8(initialContent) }
            }
        }
        // Refresh so the new file appears in the tree immediately
        _fileTree.value = scanDocumentTree(rootUriString)
        newUriString
    }

    override suspend fun moveNode(
        sourceUriString: String,
        sourceParentUriString: String,
        targetDirectoryUriString: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (sourceUriString == targetDirectoryUriString || sourceParentUriString == targetDirectoryUriString) {
            return@withContext false
        }
        val moved = try {
            DocumentsContract.moveDocument(
                context.contentResolver,
                Uri.parse(sourceUriString),
                Uri.parse(sourceParentUriString),
                Uri.parse(targetDirectoryUriString)
            ) != null
        } catch (_: Exception) {
            false
        }
        if (moved) {
            val rootUriString = storagePreferences.observeRootDirectoryUri().first()
            if (rootUriString != null) _fileTree.value = scanDocumentTree(rootUriString)
        }
        moved
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
