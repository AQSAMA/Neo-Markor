package com.aqsama.neomarkor.domain.repository

import com.aqsama.neomarkor.domain.model.FileNode
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    /** Emits the live file tree rooted at the saved SAF directory. */
    fun observeFileTree(): Flow<List<FileNode>>

    /** Emits the persisted root directory URI string (null if not yet chosen). */
    fun observeDirectoryUri(): Flow<String?>

    /** Persists the granted SAF directory URI and triggers a directory scan. */
    suspend fun setDirectoryUri(uriString: String)

    /** Re-scans the saved SAF directory and refreshes the file tree. */
    suspend fun refreshFileTree()

    /** Reads the content of a document URI using Okio. */
    suspend fun readFile(uriString: String): String

    /** Writes [content] to the document URI using Okio (truncates first). */
    suspend fun writeFile(uriString: String, content: String)

    /**
     * Creates a new file with [fileName] in the saved root directory, writes [initialContent]
     * into it, and returns the new document URI string. Returns null if no root is saved.
     */
    suspend fun createFile(fileName: String, initialContent: String = ""): String?

    /** Deletes the file or directory at the given URI. Returns true if successful. */
    suspend fun deleteFile(uriString: String): Boolean

    /** Renames the file at the given URI. Returns the new URI string or null on failure. */
    suspend fun renameFile(uriString: String, newName: String): String?

    /**
     * Moves a file into a target directory. Returns the new URI string or null on failure.
     */
    suspend fun moveFile(sourceUriString: String, targetDirectoryUriString: String): String?

    /**
     * Creates a subfolder with [folderName] inside the given parent directory
     * (or root if parent is null). Returns the new folder URI string or null.
     */
    suspend fun createFolder(folderName: String, parentUriString: String? = null): String?

    /**
     * Resolves a wiki-link target name to a file URI by searching the file tree.
     * Matches by filename (without extension). Returns null if not found.
     */
    suspend fun resolveWikiLink(target: String): String?

    /** Generates HTML from the Markdown [content]. */
    fun markdownToHtml(content: String): String
}
