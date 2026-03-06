package com.aqsama.neomarkor.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a user-managed folder for organizing notes.
 * Supports hierarchical nesting via [parentId] and manual ordering via [order].
 */
@Serializable
data class Folder(
    val id: String,
    val name: String,
    val color: Long = DEFAULT_FOLDER_COLOR,
    val parentId: String? = null,
    val order: Int = 0,
    val noteCount: Int = 0,
) {
    companion object {
        const val DEFAULT_FOLDER_COLOR = 0xFF9E9E9E // Gray
    }
}

/** Predefined color options for folder creation and editing. */
object FolderColors {
    val colors: List<Pair<String, Long>> = listOf(
        "Gray" to 0xFF9E9E9E,
        "Red" to 0xFFE53935,
        "Orange" to 0xFFFF9800,
        "Yellow" to 0xFFFFEB3B,
        "Green" to 0xFF4CAF50,
        "Teal" to 0xFF009688,
        "Blue" to 0xFF2196F3,
        "Purple" to 0xFF9C27B0,
        "Light Blue" to 0xFF03A9F4,
        "Pink" to 0xFFE91E63,
        "Brown" to 0xFF795548,
    )
}
