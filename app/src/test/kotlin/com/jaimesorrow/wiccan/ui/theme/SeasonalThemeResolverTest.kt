package com.jaimesorrow.wiccan.ui.theme

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeasonalThemeResolverTest {

    @Test
    fun `mid-period dates resolve to the pure dominant palette for each sabbat`() {
        // One date deep inside each sabbat's ~6-7 week span, well outside
        // any transition window, paired with that sabbat's palette.
        val cases = mapOf(
            LocalDate.of(2026, 1, 5) to WiccanPalette.NEAR_BLACK,
            LocalDate.of(2026, 2, 15) to WiccanPalette.MIDNIGHT_BLUE,
            LocalDate.of(2026, 4, 10) to WiccanPalette.DEEP_FOREST,
            LocalDate.of(2026, 5, 20) to WiccanPalette.ROYAL_PLUM,
            LocalDate.of(2026, 7, 10) to WiccanPalette.LIGHT_IVORY,
            LocalDate.of(2026, 8, 20) to WiccanPalette.WARM_EARTH,
            LocalDate.of(2026, 10, 10) to WiccanPalette.ROYAL_PLUM,
            LocalDate.of(2026, 11, 20) to WiccanPalette.MIDNIGHT_BLUE,
        )

        cases.forEach { (date, expected) ->
            val resolution = SeasonalThemeResolver.resolve(date)
            assertEquals("date=$date", expected, resolution.dominant)
            assertNull("date=$date should not be blending", resolution.blendTarget)
        }
    }

    @Test
    fun `a date before the year's first anchor wraps to the previous year's Yule`() {
        // Jan 15 has no anchor at/before it within the same calendar year
        // (the first anchor, Imbolc, is Feb 1) - it must resolve against
        // the *previous* year's Yule (Dec 21), not fail or default oddly.
        val resolution = SeasonalThemeResolver.resolve(LocalDate.of(2026, 1, 15))

        assertEquals(WiccanPalette.NEAR_BLACK, resolution.dominant)
        assertNull(resolution.blendTarget)
    }

    @Test
    fun `the anchor day itself is the pure new dominant, not a blend`() {
        val resolution = SeasonalThemeResolver.resolve(LocalDate.of(2026, 2, 1)) // Imbolc

        assertEquals(WiccanPalette.MIDNIGHT_BLUE, resolution.dominant)
        assertNull(resolution.blendTarget)
    }

    @Test
    fun `the week before an anchor blends toward it with a rising fraction`() {
        // Imbolc is Feb 1; Jan 26 is 6 days before it (still black. warmer 6-day mark).
        val sixDaysOut = SeasonalThemeResolver.resolve(LocalDate.of(2026, 1, 26))
        assertEquals(WiccanPalette.NEAR_BLACK, sixDaysOut.dominant)
        assertEquals(WiccanPalette.MIDNIGHT_BLUE, sixDaysOut.blendTarget)
        assertEquals(1f / 7f, sixDaysOut.blendFraction, 0.001f)

        // One day before Imbolc: fraction should be close to (but not) 1.
        val oneDayOut = SeasonalThemeResolver.resolve(LocalDate.of(2026, 1, 31))
        assertEquals(WiccanPalette.NEAR_BLACK, oneDayOut.dominant)
        assertEquals(WiccanPalette.MIDNIGHT_BLUE, oneDayOut.blendTarget)
        assertEquals(6f / 7f, oneDayOut.blendFraction, 0.001f)

        // Eight days before Imbolc: outside the 7-day transition window.
        val eightDaysOut = SeasonalThemeResolver.resolve(LocalDate.of(2026, 1, 24))
        assertEquals(WiccanPalette.NEAR_BLACK, eightDaysOut.dominant)
        assertNull(eightDaysOut.blendTarget)
    }

    @Test
    fun `resolution is deterministic - the same date always returns the same result`() {
        val date = LocalDate.of(2026, 5, 27) // inside Beltane's transition window toward Litha
        val first = SeasonalThemeResolver.resolve(date)
        val second = SeasonalThemeResolver.resolve(date)

        assertEquals(first, second)
    }

    @Test
    fun `resolution is stable across a leap year boundary`() {
        // 2028 is a leap year; Yule (Dec 21, 2027) to Imbolc (Feb 1, 2028)
        // spans Feb 29, 2028 does not exist yet at this point, but the
        // wraparound math (year - 1 / year + 1) must not throw or misfire
        // near the turn of a leap year.
        val resolution = SeasonalThemeResolver.resolve(LocalDate.of(2028, 1, 10))

        assertEquals(WiccanPalette.NEAR_BLACK, resolution.dominant)
    }
}
