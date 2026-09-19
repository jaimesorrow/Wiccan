package com.wiccan.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.wiccan.app.domain.model.WiccanThemeMode
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

    private fun contrast(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        val lighter = maxOf(firstLuminance, secondLuminance)
        val darker = minOf(firstLuminance, secondLuminance)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
