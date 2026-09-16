package com.wiccan.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Builds the full Material3 [ColorScheme] for a [WiccanPalette]. Centralizing
 * this mapping (rather than letting individual screens reach for raw
 * [WiccanPaletteColors]) is what lets every screen stay palette-agnostic:
 * they only ever read `MaterialTheme.colorScheme.*` semantic roles.
 */
internal fun WiccanPalette.toColorScheme(): ColorScheme = when (this) {
    WiccanPalette.NEAR_BLACK -> with(WiccanPaletteColors.NearBlack) {
        darkColorScheme(
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
            onSurface = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }

    WiccanPalette.MIDNIGHT_BLUE -> with(WiccanPaletteColors.MidnightBlue) {
        darkColorScheme(
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
            onSurface = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }

    WiccanPalette.ROYAL_PLUM -> with(WiccanPaletteColors.RoyalPlum) {
        darkColorScheme(
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
            onSurface = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }

    WiccanPalette.DEEP_FOREST -> with(WiccanPaletteColors.DeepForest) {
        darkColorScheme(
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
            onSurface = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }

    WiccanPalette.WARM_EARTH -> with(WiccanPaletteColors.WarmEarth) {
        darkColorScheme(
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
            onSurface = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }

    WiccanPalette.LIGHT_IVORY -> with(WiccanPaletteColors.LightIvory) {
        lightColorScheme(
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
            onSurface = OnBackground,
            onSurfaceVariant = OnSurfaceVariant,
            outline = Outline,
            error = Error,
            onError = OnError,
            errorContainer = ErrorContainer,
            onErrorContainer = OnErrorContainer,
        )
    }
}
