package com.wiccan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WiccanDarkColorScheme = darkColorScheme(
    background = WiccanColors.Background,
    surface = WiccanColors.Surface,
    surfaceVariant = WiccanColors.SurfaceElevated,
    primary = WiccanColors.DeepPurple,
    primaryContainer = WiccanColors.DeepPurpleContainer,
    onPrimary = WiccanColors.OnBackground,
    onPrimaryContainer = WiccanColors.OnBackground,
    secondary = WiccanColors.Emerald,
    onSecondary = WiccanColors.OnBackground,
    tertiary = WiccanColors.Brass,
    onTertiary = WiccanColors.Background,
    tertiaryContainer = WiccanColors.BrassMuted,
    onTertiaryContainer = WiccanColors.OnBackground,
    onBackground = WiccanColors.OnBackground,
    onSurface = WiccanColors.OnBackground,
    onSurfaceVariant = WiccanColors.OnSurfaceMuted,
    outline = WiccanColors.Outline,
    // Reuse brass rather than a jarring red — safety notes stay in-palette.
    error = WiccanColors.Brass,
    onError = WiccanColors.Background,
)

/**
 * The app is dark-only by design (Wiccan direction); no lightColorScheme
 * counterpart is offered. Callers should wrap all screens in this theme
 * rather than reaching for hardcoded colors.
 */
@Composable
fun WiccanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WiccanDarkColorScheme,
        typography = WiccanTypography,
        content = content,
    )
}
