package com.wiccan.app.ui.theme

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WiccanColorSchemeResolverTest {

    @Test
    fun `a fixed mode always resolves to its own palette regardless of system or date`() {
        val date = LocalDate.of(2026, 6, 21) // Litha - would otherwise resolve to Light Ivory
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
        // Six days before Imbolc: blendFraction ~0.14 toward Midnight Blue -
        // still closer to the dominant (Near Black).
        val mostlyDominant = LocalDate.of(2026, 1, 26)
        assertEquals(
            WiccanPalette.NEAR_BLACK,
            resolveWiccanPalette(WiccanThemeMode.AUTOMATIC_SEASONAL, isSystemInDarkTheme = false, today = mostlyDominant),
        )

        // One day before Imbolc: blendFraction ~0.86 toward Midnight Blue -
        // now closer to the target than the dominant.
        val mostlyTarget = LocalDate.of(2026, 1, 31)
        assertEquals(
            WiccanPalette.MIDNIGHT_BLUE,
            resolveWiccanPalette(WiccanThemeMode.AUTOMATIC_SEASONAL, isSystemInDarkTheme = false, today = mostlyTarget),
        )
    }
}
