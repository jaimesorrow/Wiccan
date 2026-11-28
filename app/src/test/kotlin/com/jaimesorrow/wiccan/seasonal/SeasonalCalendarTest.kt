package com.jaimesorrow.wiccan.seasonal

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity cases must stay in lockstep with
 * `functions/src/__tests__/seasonalCalendar.test.ts`.
 */
class SeasonalCalendarTest {
    @Test
    fun `sabbat takes priority over the moon window`() {
        assertEquals(SeasonalWindow.Sabbat("samhain"), SeasonalCalendar.seasonalWindowFor(LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun `full and new moons are labeled but ordinary days are quiet`() {
        assertEquals(SeasonalWindow.Moon("full"), SeasonalCalendar.seasonalWindowFor(LocalDate.of(2026, 3, 3)))
        assertEquals(SeasonalWindow.Moon("new"), SeasonalCalendar.seasonalWindowFor(LocalDate.of(2026, 8, 12)))
        assertNull(SeasonalCalendar.seasonalWindowFor(LocalDate.of(2026, 7, 7)))
    }

    @Test
    fun `moon event boundaries match the server civil date`() {
        val expected = mapOf(
            "2026-01-16" to null,
            "2026-01-17" to SeasonalWindow.Moon("new"),
            "2026-01-19" to SeasonalWindow.Moon("new"),
            "2026-01-20" to null,
            "2026-03-01" to null,
            "2026-03-02" to SeasonalWindow.Moon("full"),
            "2026-03-04" to SeasonalWindow.Moon("full"),
            "2026-03-05" to null,
            "2026-03-16" to null,
            "2026-03-17" to SeasonalWindow.Moon("new"),
            "2026-03-19" to SeasonalWindow.Moon("new"),
            "2026-03-20" to SeasonalWindow.Sabbat("ostara"),
        )
        expected.forEach { (date, event) ->
            assertEquals(date, event, SeasonalCalendar.seasonalWindowFor(LocalDate.parse(date)))
        }
    }

    @Test
    fun `local dates in non UTC zones match server event dates`() {
        val cases = listOf(
            Triple("2026-03-02T00:30:00Z", "America/Los_Angeles", null),
            Triple("2026-03-02T00:30:00Z", "UTC", SeasonalWindow.Moon("full")),
            Triple("2026-03-01T12:30:00Z", "Pacific/Kiritimati", SeasonalWindow.Moon("full")),
        )
        cases.forEach { (instant, zone, event) ->
            val date = Instant.parse(instant).atZone(ZoneId.of(zone)).toLocalDate()
            assertEquals(zone, event, SeasonalCalendar.seasonalWindowFor(date))
        }
    }

    @Test
    fun `moon track agrees with the new and full windows`() {
        val expected = mapOf(
            "2026-01-16" to 7,
            "2026-01-17" to 0,
            "2026-01-19" to 0,
            "2026-01-20" to 1,
            "2026-03-01" to 3,
            "2026-03-02" to 4,
            "2026-03-04" to 4,
            "2026-03-05" to 5,
        )
        expected.forEach { (date, index) ->
            val day = LocalDate.parse(date)
            val track = SeasonalCalendar.moonTrack(day)
            val window = SeasonalCalendar.seasonalWindowFor(day)
            assertEquals(date, index, track.trackIndex)
            when (index) {
                0 -> {
                    assertEquals(date, "New Moon", track.name)
                    assertEquals(date, SeasonalWindow.Moon("new"), window)
                }
                4 -> {
                    assertEquals(date, "Full Moon", track.name)
                    assertEquals(date, SeasonalWindow.Moon("full"), window)
                }
                else -> {
                    assertTrue(date, track.trackIndex !in setOf(0, 4))
                    assertTrue(date, window !is SeasonalWindow.Moon)
                }
            }
        }
    }
}
