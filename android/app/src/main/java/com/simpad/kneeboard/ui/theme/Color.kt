package com.simpad.kneeboard.ui.theme

import androidx.compose.ui.graphics.Color

// Default Tactical Dark Cockpit Palette
val CockpitBackground = Color(0xFF0D1117)
val CockpitSurface = Color(0xFF161B22)
val CockpitSurfaceElevated = Color(0xFF21262D)
val CockpitBorder = Color(0xFF30363D)
val CockpitPrimary = Color(0xFF00E5FF)
val CockpitPrimaryGlow = Color(0x3300E5FF)
val CockpitSecondary = Color(0xFF388BFD)
val CockpitSuccess = Color(0xFF39D353)
val CockpitWarning = Color(0xFFF0883E)
val CockpitError = Color(0xFFF85149)
val CockpitTextPrimary = Color(0xFFF0F6FC)
val CockpitTextSecondary = Color(0xFF8B949E)
val CockpitTextMuted = Color(0xFF484F58)

// NVG (Night Vision Green) Palette
val NvgBackground = Color(0xFF001205)
val NvgSurface = Color(0xFF00240B)
val NvgSurfaceElevated = Color(0xFF003812)
val NvgBorder = Color(0xFF005A1D)
val NvgPrimary = Color(0xFF00FF66)
val NvgPrimaryGlow = Color(0x4400FF66)
val NvgTextPrimary = Color(0xFFB3FFCC)
val NvgTextSecondary = Color(0xFF00CC52)
val NvgTextMuted = Color(0xFF006629)

// Red Light Aviation Cockpit Palette
val RedLightBackground = Color(0xFF120000)
val RedLightSurface = Color(0xFF240000)
val RedLightSurfaceElevated = Color(0xFF380000)
val RedLightBorder = Color(0xFF5A0000)
val RedLightPrimary = Color(0xFFFF3333)
val RedLightPrimaryGlow = Color(0x44FF3333)
val RedLightTextPrimary = Color(0xFFFFB3B3)
val RedLightTextSecondary = Color(0xFFCC0000)
val RedLightTextMuted = Color(0xFF660000)

// Day High-Contrast Light Mode
val DayBackground = Color(0xFFF6F8FA)
val DaySurface = Color(0xFFFFFFFF)
val DaySurfaceElevated = Color(0xFFEAEFF2)
val DayBorder = Color(0xFFD0D7DE)
val DayPrimary = Color(0xFF0969DA)
val DayPrimaryGlow = Color(0x330969DA)
val DayTextPrimary = Color(0xFF1F2328)
val DayTextSecondary = Color(0xFF656D76)
val DayTextMuted = Color(0xFF8C959F)

enum class CockpitLightingMode(val label: String) {
    TACTICAL_DARK("Dark Cockpit"),
    NVG_GREEN("NVG Night Green"),
    RED_LIGHT("Red Light Chart"),
    DAY_LIGHT("Day High Contrast")
}
