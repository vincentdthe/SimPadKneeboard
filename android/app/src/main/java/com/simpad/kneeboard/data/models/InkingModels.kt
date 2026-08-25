package com.simpad.kneeboard.data.models

import kotlinx.serialization.Serializable

@Serializable
data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class InkStroke(
    val id: String,
    val colorArgb: Long,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false,
    val points: List<InkPoint>
)

@Serializable
data class PageInkingData(
    val pageId: String,
    val strokes: List<InkStroke> = emptyList(),
    val lastModifiedUtc: Long = System.currentTimeMillis()
)

enum class InkingTool {
    PEN,
    HIGHLIGHTER,
    ERASER
}

data class PenPreset(
    val colorArgb: Long,
    val name: String,
    val strokeWidth: Float = 3.5f
)
