package com.simpad.kneeboard.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.simpad.kneeboard.ui.theme.LocalSimPadColors

@Composable
fun QuickNotesPage(
    modifier: Modifier = Modifier,
    pageTitle: String = "Scratchpad"
) {
    val colors = LocalSimPadColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Grid pattern background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            val gridColor = colors.border.copy(alpha = 0.35f)

            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
                x += step
            }

            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                y += step
            }
        }

        // Header watermark / template guides
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PILOT SCRATCHPAD  |  JTAC 9-LINE / ATIS / FREQUENCIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }
    }
}
