package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WiccanColorSchemeResolverTest {

    @Test
    fun `a fixed mode always resolves to its own palette regardless of system or date`() {
        val date = LocalDate.of(2026, 6, 21)
        assertEquals(
            WiccanPalette.NEAR_BLACK,
            resolveWiccanPalette(WiccanThemeMode.NEAR_BLACK, isSystemInDarkTheme = false, today = date),
        )
        assertEquals(
            WiccanPalette.WARM_EARTH,
            resolveWiccanPalette(WiccanThemeMode.WARM_EARTH, isSystemInDarkTheme = true, today = date),
        )
    }

    @Test
    fun `Follow System resolves to Near Black in dark mode and Light Ivory in light mode`() {
        assertEquals(
            WiccanPalette.NEAR_BLACK,
            resolveWiccanPalette(WiccanThemeMode.FOLLOW_SYSTEM, isSystemInDarkTheme = true),
        )
        assertEquals(
            WiccanPalette.LIGHT_IVORY,
            resolveWiccanPalette(WiccanThemeMode.FOLLOW_SYSTEM, isSystemInDarkTheme = false),
        )
    }

    @Test
    fun `Automatic Seasonal ignores system dark-light and follows the calendar`() {
        val midYule = LocalDate.of(2026, 1, 5)

        assertEquals(
            WiccanPalette.NEAR_BLACK,
            resolveWiccanPalette(WiccanThemeMode.AUTOMATIC_SEASONAL, isSystemInDarkTheme = true, today = midYule),
        )
        assertEquals(
            WiccanPalette.NEAR_BLACK,
            resolveWiccanPalette(WiccanThemeMode.AUTOMATIC_SEASONAL, isSystemInDarkTheme = false, today = midYule),
        )
    }

    @Test
    fun `during a seasonal transition, the discrete resolution snaps to whichever side is closer`() {
        val mostlyDominant = LocalDate.of(2026, 1, 26)
        assertEquals(
            WiccanPalette.NEAR_BLACK,
            resolveWiccanPalette(WiccanThemeMode.AUTOMATIC_SEASONAL, isSystemInDarkTheme = false, today = mostlyDominant),
        )

        val mostlyTarget = LocalDate.of(2026, 1, 31)
        assertEquals(
            WiccanPalette.MIDNIGHT_BLUE,
            resolveWiccanPalette(WiccanThemeMode.AUTOMATIC_SEASONAL, isSystemInDarkTheme = false, today = mostlyTarget),
        )
    }

    @Test
    fun `seasonal blends keep onBackground readable against the blended background`() {
        val blended = targetColorScheme(
            themeMode = WiccanThemeMode.AUTOMATIC_SEASONAL,
            isSystemInDarkTheme = false,
            today = LocalDate.of(2026, 6, 18),
        )

        assertTrue(contrast(blended.background, blended.onBackground) >= 4.5f)
    }

    @Test
    fun `all seasonal dates preserve normal text contrast across semantic roles`() {
        val firstDay = LocalDate.of(2026, 1, 1)
        for (day in 0..364) {
            val date = firstDay.plusDays(day.toLong())
            val scheme = targetColorScheme(WiccanThemeMode.AUTOMATIC_SEASONAL, false, date)
            val textPairs = mapOf(
                "background" to (scheme.background to scheme.onBackground),
                "surface" to (scheme.surface to scheme.onSurface),
                "surfaceVariant" to (scheme.surfaceVariant to scheme.onSurfaceVariant),
                "primary" to (scheme.primary to scheme.onPrimary),
                "primaryContainer" to (scheme.primaryContainer to scheme.onPrimaryContainer),
                "secondary" to (scheme.secondary to scheme.onSecondary),
                "secondaryContainer" to (scheme.secondaryContainer to scheme.onSecondaryContainer),
                "tertiary" to (scheme.tertiary to scheme.onTertiary),
                "tertiaryContainer" to (scheme.tertiaryContainer to scheme.onTertiaryContainer),
                "error" to (scheme.error to scheme.onError),
                "errorContainer" to (scheme.errorContainer to scheme.onErrorContainer),
                "primaryOnBackground" to (scheme.background to scheme.primary),
                "secondaryOnSurface" to (scheme.surface to scheme.secondary),
                "tertiaryOnSurfaceVariant" to (scheme.surfaceVariant to scheme.tertiary),
            )
            textPairs.forEach { (role, colors) ->
                assertTrue("$date: $role below 4.5:1", contrast(colors.first, colors.second) >= 4.5f)
            }
        }
    }

    private fun contrast(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        val lighter = maxOf(firstLuminance, secondLuminance)
        val darker = minOf(firstLuminance, secondLuminance)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
