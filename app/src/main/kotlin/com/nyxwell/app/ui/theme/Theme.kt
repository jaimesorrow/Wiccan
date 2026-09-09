package com.nyxwell.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NyxwellDarkColorScheme = darkColorScheme(
    background = NyxwellColors.Background,
    surface = NyxwellColors.Surface,
    surfaceVariant = NyxwellColors.SurfaceElevated,
    primary = NyxwellColors.DeepPurple,
    primaryContainer = NyxwellColors.DeepPurpleContainer,
    onPrimary = NyxwellColors.OnBackground,
    onPrimaryContainer = NyxwellColors.OnBackground,
    secondary = NyxwellColors.Emerald,
    onSecondary = NyxwellColors.OnBackground,
    tertiary = NyxwellColors.Brass,
    onTertiary = NyxwellColors.Background,
    tertiaryContainer = NyxwellColors.BrassMuted,
    onTertiaryContainer = NyxwellColors.OnBackground,
    onBackground = NyxwellColors.OnBackground,
    onSurface = NyxwellColors.OnBackground,
    onSurfaceVariant = NyxwellColors.OnSurfaceMuted,
    outline = NyxwellColors.Outline,
    // Reuse brass rather than a jarring red — safety notes stay in-palette.
    error = NyxwellColors.Brass,
    onError = NyxwellColors.Background,
)

/**
 * The app is dark-only by design (Wiccan direction); no lightColorScheme
 * counterpart is offered. Callers should wrap all screens in this theme
 * rather than reaching for hardcoded colors.
 */
@Composable
fun NyxwellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NyxwellDarkColorScheme,
        typography = NyxwellTypography,
        content = content,
    )
}
