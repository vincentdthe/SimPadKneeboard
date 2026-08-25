package com.simpad.kneeboard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simpad.kneeboard.data.models.InkingTool
import com.simpad.kneeboard.ui.theme.LocalSimPadColors

val DefaultPenColors = listOf(
    0xFF00E5FFL, // Cyan
    0xFFFFEA00L, // Yellow
    0xFFFF3333L, // Red
    0xFF39D353L, // Green
    0xFFFFFFFFL, // White
    0xFF161B22L  // Dark Charcoal
)

@Composable
fun InkingToolbar(
    modifier: Modifier = Modifier,
    activeTool: InkingTool,
    activeColorArgb: Long,
    activeStrokeWidth: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSelected: (InkingTool) -> Unit,
    onColorSelected: (Long) -> Unit,
    onStrokeWidthSelected: (Float) -> Unit,
    onUndoClicked: () -> Unit,
    onRedoClicked: () -> Unit,
    onClearClicked: () -> Unit
) {
    val colors = LocalSimPadColors.current

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, colors.border, RoundedCornerShape(24.dp)),
        color = colors.surfaceElevated.copy(alpha = 0.92f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pen Tool
            IconButton(
                onClick = { onToolSelected(InkingTool.PEN) },
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (activeTool == InkingTool.PEN) colors.primaryGlow else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Pen",
                    tint = if (activeTool == InkingTool.PEN) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Highlighter Tool
            IconButton(
                onClick = { onToolSelected(InkingTool.HIGHLIGHTER) },
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (activeTool == InkingTool.HIGHLIGHTER) colors.primaryGlow else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = "Highlighter",
                    tint = if (activeTool == InkingTool.HIGHLIGHTER) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Eraser Tool
            IconButton(
                onClick = { onToolSelected(InkingTool.ERASER) },
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (activeTool == InkingTool.ERASER) colors.primaryGlow else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = "Eraser",
                    tint = if (activeTool == InkingTool.ERASER) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 24.dp)
                    .background(colors.border)
            )

            // Color Palette (Visible when in Pen or Highlighter mode)
            AnimatedVisibility(visible = activeTool != InkingTool.ERASER) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DefaultPenColors.forEach { colorArgb ->
                        val isSelected = activeColorArgb == colorArgb
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(colorArgb))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) colors.primary else colors.border,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(colorArgb) }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 24.dp)
                    .background(colors.border)
            )

            // Undo Button
            IconButton(
                onClick = onUndoClicked,
                enabled = canUndo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) colors.textPrimary else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Redo Button
            IconButton(
                onClick = onRedoClicked,
                enabled = canRedo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) colors.textPrimary else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Clear Page Button
            IconButton(
                onClick = onClearClicked,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Notes",
                    tint = colors.error.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
