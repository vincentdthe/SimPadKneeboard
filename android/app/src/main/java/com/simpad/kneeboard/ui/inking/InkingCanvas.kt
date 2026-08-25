package com.simpad.kneeboard.ui.inking

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.simpad.kneeboard.data.models.InkStroke
import com.simpad.kneeboard.data.models.InkingTool

@Composable
fun InkingCanvas(
    modifier: Modifier = Modifier,
    strokes: List<InkStroke>,
    activeTool: InkingTool,
    activeColorArgb: Long,
    activeStrokeWidth: Float,
    onStrokeAdded: (InkStroke) -> Unit,
    onStrokesChanged: (List<InkStroke>) -> Unit,
    onPageSwipe: (isNext: Boolean) -> Unit
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            InkingView(context).apply {
                setOnStrokeFinishedListener { stroke ->
                    onStrokeAdded(stroke)
                }
                setOnStrokesUpdatedListener { updatedList ->
                    onStrokesChanged(updatedList)
                }
                setOnFingerSwipeListener { dx ->
                    if (dx < 0) {
                        onPageSwipe(true) // Swiped left -> Next page
                    } else {
                        onPageSwipe(false) // Swiped right -> Previous page
                    }
                }
            }
        },
        update = { view ->
            view.setTool(activeTool)
            view.setColor(activeColorArgb)
            view.setStrokeWidth(activeStrokeWidth)
            view.setStrokes(strokes)
        }
    )
}
