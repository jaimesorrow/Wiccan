package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

private val MidnightGrimoireScheme = darkColorScheme(
    background = WiccanPaletteColors.Grimoire.Background,
    surface = WiccanPaletteColors.Grimoire.Raised,
    surfaceVariant = WiccanPaletteColors.Grimoire.CardFill,
    surfaceContainerLowest = WiccanPaletteColors.Grimoire.Background,
    surfaceContainerLow = WiccanPaletteColors.Grimoire.Background,
    surfaceContainer = WiccanPaletteColors.Grimoire.Raised,
    surfaceContainerHigh = WiccanPaletteColors.Grimoire.CardFill,
    surfaceContainerHighest = WiccanPaletteColors.Grimoire.CardFill,
    primary = WiccanPaletteColors.Grimoire.Gold,
    onPrimary = WiccanPaletteColors.Grimoire.OnGold,
    primaryContainer = WiccanPaletteColors.Grimoire.Bronze,
    onPrimaryContainer = WiccanPaletteColors.Grimoire.Text,
    secondary = WiccanPaletteColors.Grimoire.Border,
    onSecondary = WiccanPaletteColors.Grimoire.OnGold,
    tertiary = WiccanPaletteColors.Grimoire.Gold,
    onTertiary = WiccanPaletteColors.Grimoire.OnGold,
    onBackground = WiccanPaletteColors.Grimoire.Text,
    onSurface = WiccanPaletteColors.Grimoire.Text,
    onSurfaceVariant = WiccanPaletteColors.Grimoire.Muted,
    outline = WiccanPaletteColors.Grimoire.Border,
    outlineVariant = WiccanPaletteColors.Grimoire.Border,
    inverseSurface = WiccanPaletteColors.Grimoire.Raised,
    inverseOnSurface = WiccanPaletteColors.Grimoire.Text,
    inversePrimary = WiccanPaletteColors.Grimoire.Gold,
)

/** Every user-facing surface uses this scheme. Seasonal palettes no longer paint the app. */
@Composable
fun MidnightGrimoireTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MidnightGrimoireScheme, typography = WiccanTypography, content = content)
}

@Composable
@ReadOnlyComposable
fun grimoireGold() = WiccanPaletteColors.Grimoire.Gold
