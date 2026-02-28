package com.aqsama.neomarkor.domain.model

data class FileNode(
    val name: String,
    val uriString: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList(),
    val lastModified: Long = 0L,
    val sizeBytes: Long = 0L,
)

/**
 * Parsed YAML frontmatter from a Markdown document.
 * Extracted from the `---` delimited block at the top of a file.
 */
data class Frontmatter(
    val title: String? = null,
    val tags: List<String> = emptyList(),
    val date: String? = null,
    val pinned: Boolean = false,
    val extra: Map<String, String> = emptyMap(),
)

/** Represents a wiki-link found in note content: [[target|display]] */
data class WikiLink(
    val target: String,
    val display: String,
    val startIndex: Int,
    val endIndex: Int,
)
