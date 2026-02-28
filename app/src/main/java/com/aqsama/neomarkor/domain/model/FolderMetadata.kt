package com.aqsama.neomarkor.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/** Preset folder colors as ARGB integers. Index 0 = default (gray). */
val FOLDER_PRESET_COLORS = listOf(
    0xFF9E9E9E.toInt(), // gray
    0xFFF44336.toInt(), // red
    0xFFFF9800.toInt(), // orange
    0xFFFFEB3B.toInt(), // yellow
    0xFF4CAF50.toInt(), // green
    0xFF009688.toInt(), // teal
    0xFF2196F3.toInt(), // blue
    0xFF9C27B0.toInt(), // purple
    0xFF03A9F4.toInt(), // light blue
    0xFFE91E63.toInt(), // pink
    0xFF795548.toInt(), // brown
)

@Serializable
data class FolderMetadata(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorArgb: Int = FOLDER_PRESET_COLORS[0],
    val parentId: String? = null,
    val orderIndex: Int = 0,
)
