package com.wiccan.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.wiccan.app.domain.model.WiccanThemeMode
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

private const val THEME_TRANSITION_DURATION_MS = 900

/**
 * Resolves [themeMode] down to a single discrete [WiccanPalette], collapsing
 * any in-progress seasonal blend to whichever side it's closer to. Used
 * where an animated cross-fade doesn't apply (e.g. the home-screen widget,
 * which re-renders on its own schedule rather than continuously).
 */
fun resolveWiccanPalette(
    themeMode: WiccanThemeMode,
    isSystemInDarkTheme: Boolean,
    today: LocalDate = LocalDate.now(),
): WiccanPalette {
    themeMode.lockedPalette?.let { return it }
    return when (themeMode) {
        WiccanThemeMode.FOLLOW_SYSTEM ->
            if (isSystemInDarkTheme) WiccanPalette.NEAR_BLACK else WiccanPalette.LIGHT_IVORY
        WiccanThemeMode.AUTOMATIC_SEASONAL -> {
            val resolution = SeasonalThemeResolver.resolve(today)
            val blendTarget = resolution.blendTarget
            if (blendTarget != null && resolution.blendFraction > 0.5f) blendTarget else resolution.dominant
        }
        else -> error("unreachable: $themeMode declares a lockedPalette")
    }
}

/** The full (un-animated) target [ColorScheme] for [themeMode] right now, blending across a seasonal transition. */
internal fun targetColorScheme(
    themeMode: WiccanThemeMode,
    isSystemInDarkTheme: Boolean,
    today: LocalDate,
): ColorScheme {
    themeMode.lockedPalette?.let { return it.toColorScheme() }
    return when (themeMode) {
        WiccanThemeMode.FOLLOW_SYSTEM ->
            (if (isSystemInDarkTheme) WiccanPalette.NEAR_BLACK else WiccanPalette.LIGHT_IVORY).toColorScheme()
        WiccanThemeMode.AUTOMATIC_SEASONAL -> {
            val resolution = SeasonalThemeResolver.resolve(today)
            val dominantScheme = resolution.dominant.toColorScheme()
            val blendTarget = resolution.blendTarget
            if (blendTarget == null || resolution.blendFraction <= 0f) {
                dominantScheme
            } else {
                lerpColorScheme(dominantScheme, blendTarget.toColorScheme(), resolution.blendFraction)
            }
        }
        else -> error("unreachable: $themeMode declares a lockedPalette")
    }
}

private fun lerpColorScheme(from: ColorScheme, to: ColorScheme, fraction: Float): ColorScheme {
    val background = lerp(from.background, to.background, fraction)
    val surface = lerp(from.surface, to.surface, fraction)
    val surfaceDim = lerp(from.surfaceDim, to.surfaceDim, fraction)
    val surfaceBright = lerp(from.surfaceBright, to.surfaceBright, fraction)
    val surfaceContainerLowest = lerp(from.surfaceContainerLowest, to.surfaceContainerLowest, fraction)
    val surfaceContainerLow = lerp(from.surfaceContainerLow, to.surfaceContainerLow, fraction)
    val surfaceContainer = lerp(from.surfaceContainer, to.surfaceContainer, fraction)
    val surfaceContainerHigh = lerp(from.surfaceContainerHigh, to.surfaceContainerHigh, fraction)
    val surfaceContainerHighest = lerp(from.surfaceContainerHighest, to.surfaceContainerHighest, fraction)
    val surfaceVariant = lerp(from.surfaceVariant, to.surfaceVariant, fraction)
    val primary = lerp(from.primary, to.primary, fraction)
    val primaryContainer = lerp(from.primaryContainer, to.primaryContainer, fraction)
    val secondary = lerp(from.secondary, to.secondary, fraction)
    val secondaryContainer = lerp(from.secondaryContainer, to.secondaryContainer, fraction)
    val tertiary = lerp(from.tertiary, to.tertiary, fraction)
    val tertiaryContainer = lerp(from.tertiaryContainer, to.tertiaryContainer, fraction)
    val inverseSurface = lerp(from.inverseSurface, to.inverseSurface, fraction)
    val error = lerp(from.error, to.error, fraction)
    val errorContainer = lerp(from.errorContainer, to.errorContainer, fraction)

    return from.copy(
        background = background,
        surface = surface,
        surfaceDim = surfaceDim,
        surfaceBright = surfaceBright,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceVariant = surfaceVariant,
        primary = primary,
        onPrimary = higherContrastColor(primary, from.onPrimary, to.onPrimary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = higherContrastColor(primaryContainer, from.onPrimaryContainer, to.onPrimaryContainer),
        secondary = secondary,
        onSecondary = higherContrastColor(secondary, from.onSecondary, to.onSecondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = higherContrastColor(
            secondaryContainer,
            from.onSecondaryContainer,
            to.onSecondaryContainer,
        ),
        tertiary = tertiary,
        onTertiary = higherContrastColor(tertiary, from.onTertiary, to.onTertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = higherContrastColor(
            tertiaryContainer,
            from.onTertiaryContainer,
            to.onTertiaryContainer,
        ),
        onBackground = higherContrastColor(background, from.onBackground, to.onBackground),
        onSurface = higherContrastColor(surface, from.onSurface, to.onSurface),
        onSurfaceVariant = higherContrastColor(surfaceVariant, from.onSurfaceVariant, to.onSurfaceVariant),
        outline = lerp(from.outline, to.outline, fraction),
        outlineVariant = lerp(from.outlineVariant, to.outlineVariant, fraction),
        scrim = lerp(from.scrim, to.scrim, fraction),
        inverseSurface = inverseSurface,
        inverseOnSurface = higherContrastColor(inverseSurface, from.inverseOnSurface, to.inverseOnSurface),
        inversePrimary = lerp(from.inversePrimary, to.inversePrimary, fraction),
        surfaceTint = lerp(from.surfaceTint, to.surfaceTint, fraction),
        error = error,
        onError = higherContrastColor(error, from.onError, to.onError),
        errorContainer = errorContainer,
        onErrorContainer = higherContrastColor(errorContainer, from.onErrorContainer, to.onErrorContainer),
    )
}

private fun higherContrastColor(background: Color, first: Color, second: Color): Color =
    if (contrastRatio(background, first) >= contrastRatio(background, second)) first else second

private fun contrastRatio(first: Color, second: Color): Float {
    val firstLuminance = first.luminance()
    val secondLuminance = second.luminance()
    val lighter = max(firstLuminance, secondLuminance)
    val darker = min(firstLuminance, secondLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * `Settings.Global.ANIMATOR_DURATION_SCALE == 0` is how a user disables
 * system animations for accessibility (Settings > Accessibility > Remove
 * animations). Compose's `animateColorAsState` doesn't honor that setting
 * on its own, so theme cross-fades check it explicitly and snap instantly
 * when it's off.
 */
@Composable
private fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}

/**
 * The animated Material3 [ColorScheme] to render right now for [themeMode].
 * Seasonal drift (see [SeasonalThemeResolver]) is already gradual day to
 * day; this additionally cross-fades over [THEME_TRANSITION_DURATION_MS]
 * whenever the resolved target changes abruptly (e.g. the user switches
 * modes), so a theme change never looks like a jump-cut. Recomposing this
 * does not recreate anything below it - screens keep their state, and
 * repositories/navigation/Firebase are untouched.
 */
@Composable
fun rememberWiccanColorScheme(
    themeMode: WiccanThemeMode,
    isSystemInDarkTheme: Boolean,
    today: LocalDate = LocalDate.now(),
): ColorScheme {
    val target = targetColorScheme(themeMode, isSystemInDarkTheme, today)
    val reducedMotion = rememberReducedMotionEnabled()
    val spec = tween<Color>(if (reducedMotion) 0 else THEME_TRANSITION_DURATION_MS)

    val background by animateColorAsState(target.background, spec, label = "wiccanBackground")
    val surface by animateColorAsState(target.surface, spec, label = "wiccanSurface")
    val surfaceDim by animateColorAsState(target.surfaceDim, spec, label = "wiccanSurfaceDim")
    val surfaceBright by animateColorAsState(target.surfaceBright, spec, label = "wiccanSurfaceBright")
    val surfaceContainerLowest by animateColorAsState(
        target.surfaceContainerLowest,
        spec,
        label = "wiccanSurfaceContainerLowest",
    )
    val surfaceContainerLow by animateColorAsState(target.surfaceContainerLow, spec, label = "wiccanSurfaceContainerLow")
    val surfaceContainer by animateColorAsState(target.surfaceContainer, spec, label = "wiccanSurfaceContainer")
    val surfaceContainerHigh by animateColorAsState(
        target.surfaceContainerHigh,
        spec,
        label = "wiccanSurfaceContainerHigh",
    )
    val surfaceContainerHighest by animateColorAsState(
        target.surfaceContainerHighest,
        spec,
        label = "wiccanSurfaceContainerHighest",
    )
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "wiccanSurfaceVariant")
    val primary by animateColorAsState(target.primary, spec, label = "wiccanPrimary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "wiccanOnPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "wiccanPrimaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "wiccanOnPrimaryContainer")
    val secondary by animateColorAsState(target.secondary, spec, label = "wiccanSecondary")
    val onSecondary by animateColorAsState(target.onSecondary, spec, label = "wiccanOnSecondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, spec, label = "wiccanSecondaryContainer")
    val onSecondaryContainer by animateColorAsState(
        target.onSecondaryContainer,
        spec,
        label = "wiccanOnSecondaryContainer",
    )
    val tertiary by animateColorAsState(target.tertiary, spec, label = "wiccanTertiary")
    val onTertiary by animateColorAsState(target.onTertiary, spec, label = "wiccanOnTertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, spec, label = "wiccanTertiaryContainer")
    val onTertiaryContainer by animateColorAsState(
        target.onTertiaryContainer,
        spec,
        label = "wiccanOnTertiaryContainer",
    )
    val onBackground by animateColorAsState(target.onBackground, spec, label = "wiccanOnBackground")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "wiccanOnSurface")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "wiccanOnSurfaceVariant")
    val outline by animateColorAsState(target.outline, spec, label = "wiccanOutline")
    val outlineVariant by animateColorAsState(target.outlineVariant, spec, label = "wiccanOutlineVariant")
    val scrim by animateColorAsState(target.scrim, spec, label = "wiccanScrim")
    val inverseSurface by animateColorAsState(target.inverseSurface, spec, label = "wiccanInverseSurface")
    val inverseOnSurface by animateColorAsState(target.inverseOnSurface, spec, label = "wiccanInverseOnSurface")
    val inversePrimary by animateColorAsState(target.inversePrimary, spec, label = "wiccanInversePrimary")
    val surfaceTint by animateColorAsState(target.surfaceTint, spec, label = "wiccanSurfaceTint")
    val error by animateColorAsState(target.error, spec, label = "wiccanError")
    val onError by animateColorAsState(target.onError, spec, label = "wiccanOnError")
    val errorContainer by animateColorAsState(target.errorContainer, spec, label = "wiccanErrorContainer")
    val onErrorContainer by animateColorAsState(target.onErrorContainer, spec, label = "wiccanOnErrorContainer")

    return target.copy(
        background = background,
        surface = surface,
        surfaceDim = surfaceDim,
        surfaceBright = surfaceBright,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceVariant = surfaceVariant,
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        onBackground = onBackground,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        surfaceTint = surfaceTint,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
    )
}
