package com.simpad.kneeboard.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.simpad.kneeboard.data.models.KneeboardPage
import com.simpad.kneeboard.ui.theme.LocalSimPadColors
import kotlin.math.roundToInt

@Composable
fun DocumentPageViewer(
    modifier: Modifier = Modifier,
    page: KneeboardPage,
    imageUrl: String,
    totalPagesInTab: Int,
    currentPageIndex: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val colors = LocalSimPadColors.current
    var scale by remember(page.id) { mutableStateOf(1f) }
    var offsetX by remember(page.id) { mutableStateOf(0f) }
    var offsetY by remember(page.id) { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(page.id) {
                // Two-finger pinch-to-zoom and pan
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Document Image Layer (PDF or Image page)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = page.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )

        // Bottom Page Indicator & Turn Controls
        if (totalPagesInTab > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = colors.surfaceElevated.copy(alpha = 0.85f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousPage,
                        enabled = currentPageIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Page",
                            tint = if (currentPageIndex > 0) colors.primary else colors.textMuted
                        )
                    }

                    Text(
                        text = "${currentPageIndex + 1} / $totalPagesInTab",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = onNextPage,
                        enabled = currentPageIndex < totalPagesInTab - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Page",
                            tint = if (currentPageIndex < totalPagesInTab - 1) colors.primary else colors.textMuted
                        )
                    }
                }
            }
        }
    }
}
