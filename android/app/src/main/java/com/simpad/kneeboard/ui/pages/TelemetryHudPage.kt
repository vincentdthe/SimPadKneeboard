package com.simpad.kneeboard.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.simpad.kneeboard.data.models.TelemetryData
import com.simpad.kneeboard.ui.theme.LocalSimPadColors

@Composable
fun TelemetryHudPage(
    modifier: Modifier = Modifier,
    telemetry: TelemetryData
) {
    val colors = LocalSimPadColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mission & Aircraft Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
            color = colors.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FLIGHT CONTEXT",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    Text(
                        text = if (telemetry.missionTitle.isNotEmpty()) telemetry.missionTitle else "Free Flight Sortie",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "THEATER / MAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    Text(
                        text = telemetry.theater,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary
                    )
                }
            }
        }

        // Active COM Radios & Navigation Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
            color = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "COMMUNICATIONS & RADIOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary
                )

                if (telemetry.radios.isNotEmpty()) {
                    telemetry.radios.forEach { (channel, freq) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceElevated, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = channel,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textSecondary
                            )
                            Text(
                                text = freq,
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.primary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No active radio frequency data reported by simulator hook.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted
                    )
                }
            }
        }

        // Bullseye & Coordinates
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bullseye Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                color = colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "BULLSEYE",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )

                    val bullseye = telemetry.bullseye
                    if (bullseye != null) {
                        Text(
                            text = String.format("%03.0f° / %.1f NM", bullseye.bearingDeg, bullseye.distanceNm),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary
                        )
                    } else {
                        Text(
                            text = "BRAA Unavailable",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted
                        )
                    }
                }
            }

            // Coordinates Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                color = colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "POSITION (LAT / LON)",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                    Text(
                        text = String.format("LAT: %.4f°", telemetry.latitude),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = String.format("LON: %.4f°", telemetry.longitude),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }
            }
        }

        // Flight Dynamics Instrument Grid
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
            color = colors.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "FLIGHT DYNAMICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InstrumentGauge("ALT MSL", String.format("%.0f FT", telemetry.altitudeFeet))
                    InstrumentGauge("RADAR AGL", String.format("%.0f FT", telemetry.aglFeet))
                    InstrumentGauge("IAS", String.format("%.0f KTS", telemetry.iasKnots))
                    InstrumentGauge("MACH", String.format("%.2f", telemetry.mach))
                    InstrumentGauge("HDG", String.format("%03.0f°", telemetry.headingDeg))
                }
            }
        }
    }
}

@Composable
private fun InstrumentGauge(label: String, value: String) {
    val colors = LocalSimPadColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary
        )
    }
}
