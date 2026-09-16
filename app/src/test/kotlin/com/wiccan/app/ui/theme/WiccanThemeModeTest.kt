package com.wiccan.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WiccanThemeModeTest {

    @Test
    fun `default mode is automatic seasonal`() {
        assertEquals(WiccanThemeMode.AUTOMATIC_SEASONAL, WiccanThemeMode.DEFAULT)
    }

    @Test
    fun `every persisted name round-trips to the same mode`() {
        WiccanThemeMode.entries.forEach { mode ->
            assertEquals(mode, WiccanThemeMode.fromPersistedName(mode.name))
        }
    }

    @Test
    fun `a missing stored value falls back to the default`() {
        assertEquals(WiccanThemeMode.DEFAULT, WiccanThemeMode.fromPersistedName(null))
    }

    @Test
    fun `a blank stored value falls back to the default`() {
        assertEquals(WiccanThemeMode.DEFAULT, WiccanThemeMode.fromPersistedName(""))
    }

    @Test
    fun `an unrecognized - eg legacy - stored value falls back to the default`() {
        // Simulates a value written by some hypothetical older schema that
        // never mapped to a current enum entry, or plain data corruption -
        // this must never crash, only fall back.
        assertEquals(WiccanThemeMode.DEFAULT, WiccanThemeMode.fromPersistedName("SYSTEM_DEFAULT"))
        assertEquals(WiccanThemeMode.DEFAULT, WiccanThemeMode.fromPersistedName("DARK"))
        assertEquals(WiccanThemeMode.DEFAULT, WiccanThemeMode.fromPersistedName("not_even_close"))
    }

    @Test
    fun `AUTOMATIC_SEASONAL and FOLLOW_SYSTEM have no locked palette`() {
        assertNull(WiccanThemeMode.AUTOMATIC_SEASONAL.lockedPalette)
        assertNull(WiccanThemeMode.FOLLOW_SYSTEM.lockedPalette)
    }

    @Test
    fun `every fixed mode locks to its same-named palette`() {
        assertEquals(WiccanPalette.NEAR_BLACK, WiccanThemeMode.NEAR_BLACK.lockedPalette)
        assertEquals(WiccanPalette.MIDNIGHT_BLUE, WiccanThemeMode.MIDNIGHT_BLUE.lockedPalette)
        assertEquals(WiccanPalette.ROYAL_PLUM, WiccanThemeMode.ROYAL_PLUM.lockedPalette)
        assertEquals(WiccanPalette.DEEP_FOREST, WiccanThemeMode.DEEP_FOREST.lockedPalette)
        assertEquals(WiccanPalette.WARM_EARTH, WiccanThemeMode.WARM_EARTH.lockedPalette)
        assertEquals(WiccanPalette.LIGHT_IVORY, WiccanThemeMode.LIGHT_IVORY.lockedPalette)
    }
}
