package com.simpad.kneeboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simpad.kneeboard.data.models.InkStroke
import com.simpad.kneeboard.data.models.InkingTool
import com.simpad.kneeboard.data.repository.InkingStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InkingUiState(
    val currentPageId: String = "",
    val strokes: List<InkStroke> = emptyList(),
    val activeTool: InkingTool = InkingTool.PEN,
    val activeColorArgb: Long = 0xFF00E5FFL,
    val activeStrokeWidth: Float = 3.5f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

class InkingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InkingStorageRepository(application)

    private val _uiState = MutableStateFlow(InkingUiState())
    val uiState: StateFlow<InkingUiState> = _uiState.asStateFlow()

    fun loadPageStrokes(pageId: String) {
        if (_uiState.value.currentPageId == pageId) return

        viewModelScope.launch {
            val strokes = repository.loadPageStrokes(pageId)
            _uiState.value = _uiState.value.copy(
                currentPageId = pageId,
                strokes = strokes,
                canUndo = false,
                canRedo = false
            )
        }
    }

    fun addStroke(stroke: InkStroke) {
        val pageId = _uiState.value.currentPageId
        if (pageId.isEmpty()) return

        val updatedStrokes = repository.addStroke(pageId, stroke)
        _uiState.value = _uiState.value.copy(
            strokes = updatedStrokes,
            canUndo = true,
            canRedo = false
        )

        viewModelScope.launch {
            repository.savePageStrokes(pageId, updatedStrokes)
        }
    }

    fun updateStrokes(strokes: List<InkStroke>) {
        val pageId = _uiState.value.currentPageId
        if (pageId.isEmpty()) return

        _uiState.value = _uiState.value.copy(strokes = strokes)
        viewModelScope.launch {
            repository.savePageStrokes(pageId, strokes)
        }
    }

    fun undo() {
        val pageId = _uiState.value.currentPageId
        val previousStrokes = repository.undo(pageId) ?: return

        _uiState.value = _uiState.value.copy(
            strokes = previousStrokes,
            canRedo = true
        )

        viewModelScope.launch {
            repository.savePageStrokes(pageId, previousStrokes)
        }
    }

    fun redo() {
        val pageId = _uiState.value.currentPageId
        val nextStrokes = repository.redo(pageId) ?: return

        _uiState.value = _uiState.value.copy(
            strokes = nextStrokes
        )

        viewModelScope.launch {
            repository.savePageStrokes(pageId, nextStrokes)
        }
    }

    fun clearCurrentPage() {
        val pageId = _uiState.value.currentPageId
        if (pageId.isEmpty()) return

        val clearedStrokes = repository.clearPageStrokes(pageId)
        _uiState.value = _uiState.value.copy(
            strokes = clearedStrokes,
            canUndo = true,
            canRedo = false
        )

        viewModelScope.launch {
            repository.savePageStrokes(pageId, clearedStrokes)
        }
    }

    fun setTool(tool: InkingTool) {
        val strokeWidth = when (tool) {
            InkingTool.PEN -> 3.5f
            InkingTool.HIGHLIGHTER -> 18.0f
            InkingTool.ERASER -> 20.0f
        }
        _uiState.value = _uiState.value.copy(
            activeTool = tool,
            activeStrokeWidth = strokeWidth
        )
    }

    fun setColor(colorArgb: Long) {
        _uiState.value = _uiState.value.copy(activeColorArgb = colorArgb)
    }

    fun setStrokeWidth(width: Float) {
        _uiState.value = _uiState.value.copy(activeStrokeWidth = width)
    }
}
