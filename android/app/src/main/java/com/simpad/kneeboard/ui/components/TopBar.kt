package com.simpad.kneeboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simpad.kneeboard.data.api.ConnectionStatus
import com.simpad.kneeboard.ui.theme.CockpitLightingMode
import com.simpad.kneeboard.ui.theme.LocalSimPadColors

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    activeSimulator: String,
    activeAircraft: String,
    connectionStatus: ConnectionStatus,
    latencyMs: Long,
    currentLightingMode: CockpitLightingMode,
    onLightingModeChanged: (CockpitLightingMode) -> Unit,
    onOpenConnectionSettings: () -> Unit
) {
    val colors = LocalSimPadColors.current
    var isLightingMenuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Title & Sim Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SIMPAD",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.primary
                )

                // Active Simulator Badge
                if (activeSimulator != "None" && activeSimulator.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flight,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = activeSimulator,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                // Active Aircraft Badge
                if (activeAircraft != "None" && activeAircraft != "Unknown" && activeAircraft.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = activeAircraft,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // Connection Status & Cockpit Theme Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .clickable { onOpenConnectionSettings() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statusDotColor = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> colors.success
                            ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> colors.warning
                            ConnectionStatus.DISCONNECTED -> colors.error
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )

                        val statusText = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> if (latencyMs > 0) "${latencyMs}ms" else "ONLINE"
                            ConnectionStatus.CONNECTING -> "CONNECTING"
                            ConnectionStatus.RECONNECTING -> "RECONNECTING"
                            ConnectionStatus.DISCONNECTED -> "OFFLINE"
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                // Cockpit Lighting Mode Switcher
                Box {
                    IconButton(
                        onClick = { isLightingMenuOpen = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness4,
                            contentDescription = "Lighting Mode",
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isLightingMenuOpen,
                        onDismissRequest = { isLightingMenuOpen = false }
                    ) {
                        CockpitLightingMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onLightingModeChanged(mode)
                                    isLightingMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // Connection Settings
                IconButton(
                    onClick = onOpenConnectionSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Connection Settings",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
