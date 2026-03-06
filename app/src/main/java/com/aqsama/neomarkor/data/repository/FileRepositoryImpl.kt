package com.aqsama.neomarkor.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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

private val SUPPORTED_EXTENSIONS = setOf("md", "txt", "json", "yaml", "yml", "todo.txt")
private const val TAG = "FileRepositoryImpl"

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
        val mimeType = mimeTypeForFileName(fileName)
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

    override suspend fun deleteFile(uriString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = DocumentFile.fromSingleUri(context, Uri.parse(uriString))
            val result = doc?.delete() ?: false
            if (result) refreshFileTree()
            result
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun renameFile(uriString: String, newName: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = DocumentFile.fromSingleUri(context, Uri.parse(uriString)) ?: return@withContext null
            if (doc.renameTo(newName)) {
                refreshFileTree()
                doc.uri.toString()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun moveFile(sourceUriString: String, targetDirectoryUriString: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val sourceDoc = DocumentFile.fromSingleUri(context, Uri.parse(sourceUriString))
                    ?: return@withContext null
                val targetDir = DocumentFile.fromTreeUri(context, Uri.parse(targetDirectoryUriString))
                    ?: return@withContext null
                val newDoc = copyDocument(sourceDoc, targetDir) ?: return@withContext null
                sourceDoc.delete()
                refreshFileTree()
                newDoc.uri.toString()
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to move document from $sourceUriString to $targetDirectoryUriString", exception)
                null
            }
        }

    override suspend fun createFolder(folderName: String, parentUriString: String?): String? =
        withContext(Dispatchers.IO) {
            val parentUri = parentUriString
                ?: storagePreferences.observeRootDirectoryUri().first()
                ?: return@withContext null
            val parentDoc = DocumentFile.fromTreeUri(context, Uri.parse(parentUri))
                ?: return@withContext null
            val newDir = parentDoc.createDirectory(folderName) ?: return@withContext null
            refreshFileTree()
            newDir.uri.toString()
        }

    override suspend fun resolveWikiLink(target: String): String? {
        val tree = _fileTree.value
        return findFileByName(tree, target)
    }

    override fun markdownToHtml(content: String): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head>")
        sb.append("<meta charset='utf-8'>")
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
        sb.append("<style>")
        sb.append("body{font-family:sans-serif;padding:16px;line-height:1.6;color:#1b1b1b;}")
        sb.append("h1,h2,h3{margin-top:1em;} code{background:#f5f5f5;padding:2px 4px;border-radius:3px;}")
        sb.append("pre{background:#f5f5f5;padding:12px;border-radius:6px;overflow-x:auto;}")
        sb.append("blockquote{border-left:3px solid #ccc;margin-left:0;padding-left:12px;color:#666;}")
        sb.append("table{border-collapse:collapse;width:100%;} th,td{border:1px solid #ddd;padding:8px;text-align:left;}")
        sb.append("img{max-width:100%;height:auto;}")
        sb.append("a{color:#0277BD;}")
        sb.append("</style></head><body>")
        sb.append(markdownBodyToHtml(content))
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun markdownBodyToHtml(md: String): String {
        val lines = md.lines()
        val sb = StringBuilder()
        var inCodeBlock = false
        var inList = false

        for (line in lines) {
            // Code blocks
            if (line.trimStart().startsWith("```") || line.trimStart().startsWith("~~~")) {
                if (inCodeBlock) {
                    sb.append("</code></pre>\n")
                    inCodeBlock = false
                } else {
                    if (inList) { sb.append("</ul>\n"); inList = false }
                    sb.append("<pre><code>")
                    inCodeBlock = true
                }
                continue
            }
            if (inCodeBlock) {
                sb.append(escapeHtml(line)).append("\n")
                continue
            }

            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (inList) { sb.append("</ul>\n"); inList = false }
                sb.append("<br>\n")
                continue
            }

            // Headings
            val headingMatch = Regex("""^(#{1,6})\s+(.*)$""").find(trimmed)
            if (headingMatch != null) {
                if (inList) { sb.append("</ul>\n"); inList = false }
                val level = headingMatch.groupValues[1].length
                val text = renderInline(headingMatch.groupValues[2])
                sb.append("<h$level>$text</h$level>\n")
                continue
            }

            // Horizontal rule
            if (trimmed.matches(Regex("""^[-*_]{3,}\s*$"""))) {
                if (inList) { sb.append("</ul>\n"); inList = false }
                sb.append("<hr>\n")
                continue
            }

            // Blockquote
            if (trimmed.startsWith("> ") || trimmed == ">") {
                if (inList) { sb.append("</ul>\n"); inList = false }
                val text = renderInline(trimmed.removePrefix(">").trim())
                sb.append("<blockquote>$text</blockquote>\n")
                continue
            }

            // Task list
            val taskMatch = Regex("""^-\s\[([ xX])\]\s(.*)$""").find(trimmed)
            if (taskMatch != null) {
                if (!inList) { sb.append("<ul style='list-style:none;padding-left:0;'>\n"); inList = true }
                val checked = taskMatch.groupValues[1].uppercase() == "X"
                val text = renderInline(taskMatch.groupValues[2])
                val checkbox = if (checked) "☑" else "☐"
                sb.append("<li>$checkbox $text</li>\n")
                continue
            }

            // Unordered list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inList) { sb.append("<ul>\n"); inList = true }
                val text = renderInline(trimmed.substring(2))
                sb.append("<li>$text</li>\n")
                continue
            }

            // Paragraph
            if (inList) { sb.append("</ul>\n"); inList = false }
            sb.append("<p>${renderInline(trimmed)}</p>\n")
        }
        if (inList) sb.append("</ul>\n")
        if (inCodeBlock) sb.append("</code></pre>\n")
        return sb.toString()
    }

    private fun renderInline(text: String): String {
        var result = escapeHtml(text)
        // Images: ![alt](src)
        result = result.replace(Regex("""!\[([^\]]*?)\]\(([^)]+?)\)""")) {
            "<img src='${it.groupValues[2]}' alt='${it.groupValues[1]}'>"
        }
        // Links: [text](url)
        result = result.replace(Regex("""\[([^\]]+?)\]\(([^)]+?)\)""")) {
            "<a href='${it.groupValues[2]}'>${it.groupValues[1]}</a>"
        }
        // Wiki-links: [[target|display]] or [[target]]
        result = result.replace(Regex("""\[\[([^\]]+?)(?:\|([^\]]+?))?\]\]""")) {
            val target = it.groupValues[1]
            val display = it.groupValues[2].ifEmpty { target }
            "<a href='wikilink://$target'>$display</a>"
        }
        // Bold: **text** or __text__
        result = result.replace(Regex("""\*\*(.+?)\*\*|__(.+?)__""")) {
            val content = it.groupValues[1].ifEmpty { it.groupValues[2] }
            "<strong>$content</strong>"
        }
        // Italic: *text* or _text_
        result = result.replace(Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)|(?<!_)_(?!_)(.+?)(?<!_)_(?!_)""")) {
            val content = it.groupValues[1].ifEmpty { it.groupValues[2] }
            "<em>$content</em>"
        }
        // Strikethrough: ~~text~~
        result = result.replace(Regex("""~~(.+?)~~""")) { "<del>${it.groupValues[1]}</del>" }
        // Inline code: `code`
        result = result.replace(Regex("""`([^`]+?)`""")) { "<code>${it.groupValues[1]}</code>" }
        return result
    }

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun findFileByName(nodes: List<FileNode>, target: String): String? {
        for (node in nodes) {
            if (!node.isDirectory) {
                val nameWithoutExt = node.name.substringBeforeLast(".")
                if (nameWithoutExt.equals(target, ignoreCase = true)) {
                    return node.uriString
                }
            }
            if (node.isDirectory) {
                val found = findFileByName(node.children, target)
                if (found != null) return found
            }
        }
        return null
    }

    private fun scanDocumentTree(uriString: String): List<FileNode> {
        val uri = Uri.parse(uriString)
        val rootDocument = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
        return buildChildren(rootDocument)
    }

    private fun copyDocument(source: DocumentFile, targetDir: DocumentFile): DocumentFile? {
        val name = source.name ?: return null
        return if (source.isDirectory) {
            val newDir = targetDir.createDirectory(name) ?: return null
            source.listFiles().forEach { child ->
                if (copyDocument(child, newDir) == null) {
                    Log.w(TAG, "Failed to copy child ${child.uri} into ${newDir.uri}")
                    return null
                }
            }
            newDir
        } else {
            val mimeType = mimeTypeForFileName(name)
            val newFile = targetDir.createFile(mimeType, name) ?: return null
            context.contentResolver.openInputStream(source.uri)?.use { input ->
                context.contentResolver.openOutputStream(newFile.uri, "wt")?.use { output ->
                    input.copyTo(output)
                }
            }
            newFile
        }
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
                        lastModified = doc.lastModified(),
                    )
                    doc.isFile && isSupportedFile(name) ->
                        FileNode(
                            name = name,
                            uriString = doc.uri.toString(),
                            isDirectory = false,
                            lastModified = doc.lastModified(),
                            sizeBytes = doc.length(),
                        )
                    else -> null
                }
            }
            .sortedWith(compareByDescending<FileNode> { it.isDirectory }.thenBy { it.name })

    private fun isSupportedFile(name: String): Boolean {
        if (!name.contains('.')) return false
        val ext = name.substringAfterLast('.').lowercase()
        // Handle special case: "todo.txt"
        if (name.lowercase() == "todo.txt") return true
        return ext in SUPPORTED_EXTENSIONS
    }

    private fun mimeTypeForFileName(fileName: String): String {
        val ext = fileName.substringAfterLast('.').lowercase()
        return when (ext) {
            "md" -> "text/markdown"
            "json" -> "application/json"
            "yaml", "yml" -> "text/yaml"
            else -> "text/plain"
        }
    }
}
