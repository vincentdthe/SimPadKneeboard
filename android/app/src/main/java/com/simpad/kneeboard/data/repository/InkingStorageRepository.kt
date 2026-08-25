package com.simpad.kneeboard.data.repository

import android.content.Context
import android.util.Log
import com.simpad.kneeboard.data.models.InkStroke
import com.simpad.kneeboard.data.models.PageInkingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class InkingStorageRepository(private val context: Context) {
    private val tag = "InkingStorage"
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }
    private val inkingDir: File by lazy {
        File(context.filesDir, "inking_notes").apply {
            if (!exists()) mkdirs()
        }
    }

    // In-memory cache for instant switching between kneeboard pages
    private val strokeCache = mutableMapOf<String, MutableList<InkStroke>>()
    private val undoStackMap = mutableMapOf<String, MutableList<List<InkStroke>>>()
    private val redoStackMap = mutableMapOf<String, MutableList<List<InkStroke>>>()

    suspend fun loadPageStrokes(pageId: String): List<InkStroke> = withContext(Dispatchers.IO) {
        if (strokeCache.containsKey(pageId)) {
            return@withContext strokeCache[pageId]?.toList() ?: emptyList()
        }

        val file = getPageFile(pageId)
        if (!file.exists()) {
            strokeCache[pageId] = mutableListOf()
            return@withContext emptyList()
        }

        try {
            val content = file.readText()
            val data = json.decodeFromString<PageInkingData>(content)
            strokeCache[pageId] = data.strokes.toMutableList()
            data.strokes
        } catch (e: Exception) {
            Log.e(tag, "Error loading strokes for page $pageId: ${e.message}")
            emptyList()
        }
    }

    suspend fun savePageStrokes(pageId: String, strokes: List<InkStroke>) = withContext(Dispatchers.IO) {
        strokeCache[pageId] = strokes.toMutableList()
        try {
            val file = getPageFile(pageId)
            val data = PageInkingData(
                pageId = pageId,
                strokes = strokes,
                lastModifiedUtc = System.currentTimeMillis()
            )
            val content = json.encodeToString(data)
            file.writeText(content)
        } catch (e: Exception) {
            Log.e(tag, "Error saving strokes for page $pageId: ${e.message}")
        }
    }

    fun addStroke(pageId: String, stroke: InkStroke): List<InkStroke> {
        val current = strokeCache.getOrPut(pageId) { mutableListOf() }
        val undoStack = undoStackMap.getOrPut(pageId) { mutableListOf() }
        val redoStack = redoStackMap.getOrPut(pageId) { mutableListOf() }

        // Push previous state to undo stack
        undoStack.add(current.toList())
        redoStack.clear()

        current.add(stroke)
        return current.toList()
    }

    fun undo(pageId: String): List<InkStroke>? {
        val undoStack = undoStackMap[pageId] ?: return null
        if (undoStack.isEmpty()) return null

        val current = strokeCache.getOrPut(pageId) { mutableListOf() }
        val redoStack = redoStackMap.getOrPut(pageId) { mutableListOf() }

        redoStack.add(current.toList())
        val previousState = undoStack.removeAt(undoStack.lastIndex)
        current.clear()
        current.addAll(previousState)
        return current.toList()
    }

    fun redo(pageId: String): List<InkStroke>? {
        val redoStack = redoStackMap[pageId] ?: return null
        if (redoStack.isEmpty()) return null

        val current = strokeCache.getOrPut(pageId) { mutableListOf() }
        val undoStack = undoStackMap.getOrPut(pageId) { mutableListOf() }

        undoStack.add(current.toList())
        val nextState = redoStack.removeAt(redoStack.lastIndex)
        current.clear()
        current.addAll(nextState)
        return current.toList()
    }

    fun clearPageStrokes(pageId: String): List<InkStroke> {
        val current = strokeCache.getOrPut(pageId) { mutableListOf() }
        val undoStack = undoStackMap.getOrPut(pageId) { mutableListOf() }
        val redoStack = redoStackMap.getOrPut(pageId) { mutableListOf() }

        if (current.isNotEmpty()) {
            undoStack.add(current.toList())
            redoStack.clear()
            current.clear()
        }
        return emptyList()
    }

    private fun getPageFile(pageId: String): File {
        val safeName = pageId.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".json"
        return File(inkingDir, safeName)
    }
}
