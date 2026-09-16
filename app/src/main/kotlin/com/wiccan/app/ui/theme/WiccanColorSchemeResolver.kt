package com.wiccan.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

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
private fun targetColorScheme(
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

private fun lerpColorScheme(from: ColorScheme, to: ColorScheme, fraction: Float): ColorScheme = from.copy(
    background = lerp(from.background, to.background, fraction),
    surface = lerp(from.surface, to.surface, fraction),
    surfaceVariant = lerp(from.surfaceVariant, to.surfaceVariant, fraction),
    primary = lerp(from.primary, to.primary, fraction),
    onPrimary = lerp(from.onPrimary, to.onPrimary, fraction),
    primaryContainer = lerp(from.primaryContainer, to.primaryContainer, fraction),
    onPrimaryContainer = lerp(from.onPrimaryContainer, to.onPrimaryContainer, fraction),
    secondary = lerp(from.secondary, to.secondary, fraction),
    onSecondary = lerp(from.onSecondary, to.onSecondary, fraction),
    secondaryContainer = lerp(from.secondaryContainer, to.secondaryContainer, fraction),
    onSecondaryContainer = lerp(from.onSecondaryContainer, to.onSecondaryContainer, fraction),
    tertiary = lerp(from.tertiary, to.tertiary, fraction),
    onTertiary = lerp(from.onTertiary, to.onTertiary, fraction),
    tertiaryContainer = lerp(from.tertiaryContainer, to.tertiaryContainer, fraction),
    onTertiaryContainer = lerp(from.onTertiaryContainer, to.onTertiaryContainer, fraction),
    onBackground = lerp(from.onBackground, to.onBackground, fraction),
    onSurface = lerp(from.onSurface, to.onSurface, fraction),
    onSurfaceVariant = lerp(from.onSurfaceVariant, to.onSurfaceVariant, fraction),
    outline = lerp(from.outline, to.outline, fraction),
    error = lerp(from.error, to.error, fraction),
    onError = lerp(from.onError, to.onError, fraction),
    errorContainer = lerp(from.errorContainer, to.errorContainer, fraction),
    onErrorContainer = lerp(from.onErrorContainer, to.onErrorContainer, fraction),
)

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
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
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
    val target = remember(themeMode, isSystemInDarkTheme, today) {
        targetColorScheme(themeMode, isSystemInDarkTheme, today)
    }
    val reducedMotion = rememberReducedMotionEnabled()
    val spec = tween<Color>(if (reducedMotion) 0 else THEME_TRANSITION_DURATION_MS)

    val background by animateColorAsState(target.background, spec, label = "wiccanBackground")
    val surface by animateColorAsState(target.surface, spec, label = "wiccanSurface")
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
    val error by animateColorAsState(target.error, spec, label = "wiccanError")
    val onError by animateColorAsState(target.onError, spec, label = "wiccanOnError")
    val errorContainer by animateColorAsState(target.errorContainer, spec, label = "wiccanErrorContainer")
    val onErrorContainer by animateColorAsState(target.onErrorContainer, spec, label = "wiccanOnErrorContainer")

    return target.copy(
        background = background,
        surface = surface,
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
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
    )
}
