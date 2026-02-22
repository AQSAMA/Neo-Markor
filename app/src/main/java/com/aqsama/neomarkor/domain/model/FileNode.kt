package com.aqsama.neomarkor.domain.model

data class FileNode(
    val name: String,
    val uriString: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList(),
)
