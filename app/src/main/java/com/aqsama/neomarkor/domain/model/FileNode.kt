package com.aqsama.neomarkor.domain.model

data class FileNode(
    val name: String,
    val uriString: String,
    val isDirectory: Boolean,
    /** SAF document URI of this node's immediate parent directory; null for root children. */
    val parentUriString: String? = null,
    val children: List<FileNode> = emptyList(),
)
