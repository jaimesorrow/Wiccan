package com.jaimesorrow.wiccan.ui.content

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeasonalChipTest {
    @Test
    fun `sabbat takes priority over the moon label`() {
        assertEquals("Samhain", seasonalChipText(LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun `full and new moons are labeled but ordinary days are quiet`() {
        assertEquals("Full Moon", seasonalChipText(LocalDate.of(2026, 3, 3)))
        assertEquals("New Moon", seasonalChipText(LocalDate.of(2026, 8, 12)))
        assertNull(seasonalChipText(LocalDate.of(2026, 7, 7)))
    }

    @Test
    fun `moon event boundaries match the server civil date`() {
        val expected = mapOf(
            "2026-01-16" to null,
            "2026-01-17" to "New Moon",
            "2026-01-19" to "New Moon",
            "2026-01-20" to null,
            "2026-03-01" to null,
            "2026-03-02" to "Full Moon",
            "2026-03-04" to "Full Moon",
            "2026-03-05" to null,
            "2026-03-16" to null,
            "2026-03-17" to "New Moon",
            "2026-03-19" to "New Moon",
            "2026-03-20" to "Ostara",
        )
        expected.forEach { (date, event) ->
            assertEquals(date, event, seasonalChipText(LocalDate.parse(date)))
        }
    }

    @Test
    fun `local dates in non UTC zones match server event dates`() {
        val cases = listOf(
            Triple("2026-03-02T00:30:00Z", "America/Los_Angeles", null),
            Triple("2026-03-02T00:30:00Z", "UTC", "Full Moon"),
            Triple("2026-03-01T12:30:00Z", "Pacific/Kiritimati", "Full Moon"),
        )
        cases.forEach { (instant, zone, event) ->
            val date = Instant.parse(instant).atZone(ZoneId.of(zone)).toLocalDate()
            assertEquals(zone, event, seasonalChipText(date))
        }
    }
}
