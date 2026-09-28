package com.wiccan.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Builds the full Material3 [ColorScheme] for a [WiccanPalette]. Centralizing
 * this mapping (rather than letting individual screens reach for raw
 * [WiccanPaletteColors]) is what lets every screen stay palette-agnostic:
 * they only ever read `MaterialTheme.colorScheme.*` semantic roles.
 */
internal fun WiccanPalette.toColorScheme(): ColorScheme = tokens().toColorScheme(isLight = isLight)

private data class WiccanColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
)

private fun WiccanPalette.tokens(): WiccanColorTokens = when (this) {
    WiccanPalette.NEAR_BLACK -> with(WiccanPaletteColors.NearBlack) {
        WiccanColorTokens(
            background = Background,
            surface = Surface,
            surfaceVariant = SurfaceVariant,
            primary = Primary,
            onPrimary = OnPrimary,
            primaryContainer = PrimaryContainer,
            onPrimaryContainer = OnPrimaryContainer,
            secondary = Secondary,
            onSecondary = OnSecondary,
            secondaryContainer = SecondaryContainer,
            onSecondaryContainer = OnSecondaryContainer,
            tertiary = Tertiary,
            onTertiary = OnTertiary,
            tertiaryContainer = TertiaryContainer,
            onTertiaryContainer = OnTertiaryContainer,
            onBackground = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }
    WiccanPalette.MIDNIGHT_BLUE -> with(WiccanPaletteColors.MidnightBlue) {
        WiccanColorTokens(
            background = Background,
            surface = Surface,
            surfaceVariant = SurfaceVariant,
            primary = Primary,
            onPrimary = OnPrimary,
            primaryContainer = PrimaryContainer,
            onPrimaryContainer = OnPrimaryContainer,
            secondary = Secondary,
            onSecondary = OnSecondary,
            secondaryContainer = SecondaryContainer,
            onSecondaryContainer = OnSecondaryContainer,
            tertiary = Tertiary,
            onTertiary = OnTertiary,
            tertiaryContainer = TertiaryContainer,
            onTertiaryContainer = OnTertiaryContainer,
            onBackground = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }
    WiccanPalette.ROYAL_PLUM -> with(WiccanPaletteColors.RoyalPlum) {
        WiccanColorTokens(
            background = Background,
            surface = Surface,
            surfaceVariant = SurfaceVariant,
            primary = Primary,
            onPrimary = OnPrimary,
            primaryContainer = PrimaryContainer,
            onPrimaryContainer = OnPrimaryContainer,
            secondary = Secondary,
            onSecondary = OnSecondary,
            secondaryContainer = SecondaryContainer,
            onSecondaryContainer = OnSecondaryContainer,
            tertiary = Tertiary,
            onTertiary = OnTertiary,
            tertiaryContainer = TertiaryContainer,
            onTertiaryContainer = OnTertiaryContainer,
            onBackground = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }
    WiccanPalette.DEEP_FOREST -> with(WiccanPaletteColors.DeepForest) {
        WiccanColorTokens(
            background = Background,
            surface = Surface,
            surfaceVariant = SurfaceVariant,
            primary = Primary,
            onPrimary = OnPrimary,
            primaryContainer = PrimaryContainer,
            onPrimaryContainer = OnPrimaryContainer,
            secondary = Secondary,
            onSecondary = OnSecondary,
            secondaryContainer = SecondaryContainer,
            onSecondaryContainer = OnSecondaryContainer,
            tertiary = Tertiary,
            onTertiary = OnTertiary,
            tertiaryContainer = TertiaryContainer,
            onTertiaryContainer = OnTertiaryContainer,
            onBackground = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }
    WiccanPalette.WARM_EARTH -> with(WiccanPaletteColors.WarmEarth) {
        WiccanColorTokens(
            background = Background,
            surface = Surface,
            surfaceVariant = SurfaceVariant,
            primary = Primary,
            onPrimary = OnPrimary,
            primaryContainer = PrimaryContainer,
            onPrimaryContainer = OnPrimaryContainer,
            secondary = Secondary,
            onSecondary = OnSecondary,
            secondaryContainer = SecondaryContainer,
            onSecondaryContainer = OnSecondaryContainer,
            tertiary = Tertiary,
            onTertiary = OnTertiary,
            tertiaryContainer = TertiaryContainer,
            onTertiaryContainer = OnTertiaryContainer,
            onBackground = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }
    WiccanPalette.LIGHT_IVORY -> with(WiccanPaletteColors.LightIvory) {
        WiccanColorTokens(
            background = Background,
            surface = Surface,
            surfaceVariant = SurfaceVariant,
            primary = Primary,
            onPrimary = OnPrimary,
            primaryContainer = PrimaryContainer,
            onPrimaryContainer = OnPrimaryContainer,
            secondary = Secondary,
            onSecondary = OnSecondary,
            secondaryContainer = SecondaryContainer,
            onSecondaryContainer = OnSecondaryContainer,
            tertiary = Tertiary,
            onTertiary = OnTertiary,
            tertiaryContainer = TertiaryContainer,
            onTertiaryContainer = OnTertiaryContainer,
            onBackground = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }
}

private fun WiccanColorTokens.toColorScheme(isLight: Boolean): ColorScheme {
    val inverseTokens = if (isLight) WiccanPalette.NEAR_BLACK.tokens() else WiccanPalette.LIGHT_IVORY.tokens()
    return if (isLight) {
        lightColorScheme(
            background = background,
            surface = surface,
            surfaceDim = surface,
            surfaceBright = background,
            surfaceContainerLowest = background,
            surfaceContainerLow = lerp(background, surface, 0.55f),
            surfaceContainer = surface,
            surfaceContainerHigh = lerp(surface, surfaceVariant, 0.45f),
            surfaceContainerHighest = surfaceVariant,
            surfaceVariant = surfaceVariant,
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inverseTokens.primary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            onBackground = onBackground,
            onSurface = onBackground,
            onSurfaceVariant = onSurfaceVariant,
            inverseSurface = inverseTokens.surface,
            inverseOnSurface = inverseTokens.onBackground,
            outline = outline,
            outlineVariant = lerp(surfaceVariant, outline, 0.45f),
            scrim = onBackground.copy(alpha = 0.32f),
            surfaceTint = primary,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
        )
    } else {
        darkColorScheme(
            background = background,
            surface = surface,
            surfaceDim = background,
            surfaceBright = surfaceVariant,
            surfaceContainerLowest = background,
            surfaceContainerLow = lerp(background, surface, 0.55f),
            surfaceContainer = surface,
            surfaceContainerHigh = lerp(surface, surfaceVariant, 0.45f),
            surfaceContainerHighest = surfaceVariant,
            surfaceVariant = surfaceVariant,
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inverseTokens.primary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            onBackground = onBackground,
            onSurface = onBackground,
            onSurfaceVariant = onSurfaceVariant,
            inverseSurface = inverseTokens.surface,
            inverseOnSurface = inverseTokens.onBackground,
            outline = outline,
            outlineVariant = lerp(surfaceVariant, outline, 0.45f),
            scrim = onBackground.copy(alpha = 0.32f),
            surfaceTint = primary,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
        )
    }
}
