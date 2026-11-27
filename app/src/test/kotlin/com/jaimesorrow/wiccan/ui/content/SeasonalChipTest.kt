package com.jaimesorrow.wiccan.ui.content

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeasonalChipTest {
    @Test
    fun `sabbat takes priority over the moon label`() {
        assertEquals("Samhain", seasonalChipText(LocalDate.of(2026, 10, 31), ZoneOffset.UTC))
    }

    @Test
    fun `full and new moons are labeled but ordinary days are quiet`() {
        assertEquals("Full Moon", seasonalChipText(LocalDate.of(2026, 3, 3), ZoneOffset.UTC))
        assertEquals("New Moon", seasonalChipText(LocalDate.of(2026, 8, 12), ZoneOffset.UTC))
        assertNull(seasonalChipText(LocalDate.of(2026, 7, 7), ZoneOffset.UTC))
    }
}
