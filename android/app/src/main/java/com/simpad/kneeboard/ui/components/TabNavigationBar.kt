package com.simpad.kneeboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.simpad.kneeboard.data.models.KneeboardTab
import com.simpad.kneeboard.ui.theme.LocalSimPadColors

@Composable
fun TabNavigationBar(
    modifier: Modifier = Modifier,
    tabs: List<KneeboardTab>,
    selectedTabId: String?,
    onTabSelected: (KneeboardTab) -> Unit
) {
    val colors = LocalSimPadColors.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.background,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.id == selectedTabId

                val icon: ImageVector = when {
                    tab.isDynamic && tab.dynamicType == "Radio" -> Icons.Default.Sensors
                    tab.isDynamic && tab.dynamicType == "QuickNotes" -> Icons.Default.EditNote
                    tab.category == 5 || tab.dynamicType == "WebView" -> Icons.Default.Language
                    tab.category == 0 -> Icons.Default.Public
                    tab.category == 1 -> Icons.Default.Flight
                    else -> Icons.Default.Description
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.surfaceElevated else colors.surface)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.primary else colors.border,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )

                        Text(
                            text = tab.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )

                        if (tab.pageCount > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.background)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${tab.pageCount}p",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
