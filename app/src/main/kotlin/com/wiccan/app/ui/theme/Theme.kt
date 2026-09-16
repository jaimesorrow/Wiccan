package com.wiccan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.time.LocalDate

private fun darkScheme(
    background: Long,
    surface: Long,
    elevated: Long,
    primary: Long,
    primaryContainer: Long,
    secondary: Long,
    tertiary: Long,
    onBackground: Long,
    muted: Long,
    outline: Long,
) = darkColorScheme(
    background = Color(background),
    surface = Color(surface),
    surfaceVariant = Color(elevated),
    primary = Color(primary),
    primaryContainer = Color(primaryContainer),
    onPrimary = Color(onBackground),
    onPrimaryContainer = Color(onBackground),
    secondary = Color(secondary),
    onSecondary = Color(onBackground),
    tertiary = Color(tertiary),
    onTertiary = Color(background),
    onBackground = Color(onBackground),
    onSurface = Color(onBackground),
    onSurfaceVariant = Color(muted),
    outline = Color(outline),
    error = Color(tertiary),
    onError = Color(background),
)

private val NearBlack = darkScheme(0xFF0A0810, 0xFF14101C, 0xFF1E1826, 0xFFA78BFA, 0xFF3A2D5A, 0xFF5B8770, 0xFFB8925B, 0xFFE8E1D4, 0xFFA89EB0, 0xFF2B2436)
private val MidnightBlue = darkScheme(0xFF081328, 0xFF101F38, 0xFF172B4D, 0xFF8AB4F8, 0xFF254B87, 0xFF6F9FE8, 0xFFB8D4FF, 0xFFE8F1FF, 0xFFAABBD3, 0xFF294466)
private val RoyalPlum = darkScheme(0xFF200D2A, 0xFF30133B, 0xFF43204F, 0xFFD8B4FE, 0xFF6B2F84, 0xFFA56BE8, 0xFFE2C1F2, 0xFFF3E8FF, 0xFFC7AECF, 0xFF5A3263)
private val DeepForest = darkScheme(0xFF081F17, 0xFF102B21, 0xFF17382B, 0xFFA7D7A5, 0xFF245C45, 0xFF65C99A, 0xFFC7E8D3, 0xFFE8F5EC, 0xFFA8C4B2, 0xFF315744)
private val WarmEarth = darkScheme(0xFF1C1A17, 0xFF2A241D, 0xFF3A3128, 0xFFD6C3A1, 0xFF66513A, 0xFFB79A72, 0xFFE4C997, 0xFFF5EFE6, 0xFFC4B7A4, 0xFF55483A)
private val LightIvory = lightColorScheme(
    background = Color(0xFFFAF9F6),
    surface = Color(0xFFFFFDF8),
    surfaceVariant = Color(0xFFE8E2D9),
    primary = Color(0xFF665A43),
    primaryContainer = Color(0xFFE5D9C4),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF241F17),
    secondary = Color(0xFF6D7760),
    onSecondary = Color.White,
    tertiary = Color(0xFF8B7355),
    onTertiary = Color.White,
    onBackground = Color(0xFF2D2A26),
    onSurface = Color(0xFF2D2A26),
    onSurfaceVariant = Color(0xFF625D55),
    outline = Color(0xFF9D9589),
)

private fun WiccanPalette.scheme(): ColorScheme = when (this) {
    WiccanPalette.NEAR_BLACK -> NearBlack
    WiccanPalette.MIDNIGHT_BLUE -> MidnightBlue
    WiccanPalette.ROYAL_PLUM -> RoyalPlum
    WiccanPalette.DEEP_FOREST -> DeepForest
    WiccanPalette.WARM_EARTH -> WarmEarth
    WiccanPalette.LIGHT_IVORY -> LightIvory
}

@Composable
fun WiccanTheme(
    mode: WiccanThemeMode = WiccanThemeMode.AUTOMATIC,
    date: LocalDate = LocalDate.now(),
    content: @Composable () -> Unit,
) {
    val palette = when (mode) {
        WiccanThemeMode.AUTOMATIC -> SeasonalThemeResolver.resolve(date)
        WiccanThemeMode.NEAR_BLACK -> WiccanPalette.NEAR_BLACK
        WiccanThemeMode.MIDNIGHT_BLUE -> WiccanPalette.MIDNIGHT_BLUE
        WiccanThemeMode.ROYAL_PLUM -> WiccanPalette.ROYAL_PLUM
        WiccanThemeMode.DEEP_FOREST -> WiccanPalette.DEEP_FOREST
        WiccanThemeMode.WARM_EARTH -> WiccanPalette.WARM_EARTH
        WiccanThemeMode.LIGHT_IVORY -> WiccanPalette.LIGHT_IVORY
        WiccanThemeMode.FOLLOW_SYSTEM -> if (isSystemInDarkTheme()) WiccanPalette.NEAR_BLACK else WiccanPalette.LIGHT_IVORY
    }
    MaterialTheme(
        colorScheme = palette.scheme(),
        typography = WiccanTypography,
        content = content,
    )
}
