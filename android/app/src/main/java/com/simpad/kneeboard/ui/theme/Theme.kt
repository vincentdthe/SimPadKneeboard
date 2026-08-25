package com.simpad.kneeboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class SimPadColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val primary: Color,
    val primaryGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val error: Color
)

val LocalSimPadColors = staticCompositionLocalOf {
    SimPadColors(
        background = CockpitBackground,
        surface = CockpitSurface,
        surfaceElevated = CockpitSurfaceElevated,
        border = CockpitBorder,
        primary = CockpitPrimary,
        primaryGlow = CockpitPrimaryGlow,
        textPrimary = CockpitTextPrimary,
        textSecondary = CockpitTextSecondary,
        textMuted = CockpitTextMuted,
        success = CockpitSuccess,
        warning = CockpitWarning,
        error = CockpitError
    )
}

@Composable
fun SimPadTheme(
    mode: CockpitLightingMode = CockpitLightingMode.TACTICAL_DARK,
    content: @Composable () -> Unit
) {
    val simpadColors = when (mode) {
        CockpitLightingMode.TACTICAL_DARK -> SimPadColors(
            background = CockpitBackground,
            surface = CockpitSurface,
            surfaceElevated = CockpitSurfaceElevated,
            border = CockpitBorder,
            primary = CockpitPrimary,
            primaryGlow = CockpitPrimaryGlow,
            textPrimary = CockpitTextPrimary,
            textSecondary = CockpitTextSecondary,
            textMuted = CockpitTextMuted,
            success = CockpitSuccess,
            warning = CockpitWarning,
            error = CockpitError
        )
        CockpitLightingMode.NVG_GREEN -> SimPadColors(
            background = NvgBackground,
            surface = NvgSurface,
            surfaceElevated = NvgSurfaceElevated,
            border = NvgBorder,
            primary = NvgPrimary,
            primaryGlow = NvgPrimaryGlow,
            textPrimary = NvgTextPrimary,
            textSecondary = NvgTextSecondary,
            textMuted = NvgTextMuted,
            success = NvgPrimary,
            warning = Color(0xFF88FF00),
            error = Color(0xFFFF5555)
        )
        CockpitLightingMode.RED_LIGHT -> SimPadColors(
            background = RedLightBackground,
            surface = RedLightSurface,
            surfaceElevated = RedLightSurfaceElevated,
            border = RedLightBorder,
            primary = RedLightPrimary,
            primaryGlow = RedLightPrimaryGlow,
            textPrimary = RedLightTextPrimary,
            textSecondary = RedLightTextSecondary,
            textMuted = RedLightTextMuted,
            success = Color(0xFFFF8888),
            warning = Color(0xFFFF6600),
            error = RedLightPrimary
        )
        CockpitLightingMode.DAY_LIGHT -> SimPadColors(
            background = DayBackground,
            surface = DaySurface,
            surfaceElevated = DaySurfaceElevated,
            border = DayBorder,
            primary = DayPrimary,
            primaryGlow = DayPrimaryGlow,
            textPrimary = DayTextPrimary,
            textSecondary = DayTextSecondary,
            textMuted = DayTextMuted,
            success = Color(0xFF1A7F37),
            warning = Color(0xFF9A6700),
            error = Color(0xFFCF222E)
        )
    }

    val materialColors = if (mode == CockpitLightingMode.DAY_LIGHT) {
        lightColorScheme(
            background = simpadColors.background,
            surface = simpadColors.surface,
            primary = simpadColors.primary,
            onBackground = simpadColors.textPrimary,
            onSurface = simpadColors.textPrimary
        )
    } else {
        darkColorScheme(
            background = simpadColors.background,
            surface = simpadColors.surface,
            primary = simpadColors.primary,
            onBackground = simpadColors.textPrimary,
            onSurface = simpadColors.textPrimary
        )
    }

    CompositionLocalProvider(LocalSimPadColors provides simpadColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = CockpitTypography,
            content = content
        )
    }
}
