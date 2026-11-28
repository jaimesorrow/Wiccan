package com.jaimesorrow.wiccan.seasonal

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Primary Moon phases for late 2026 from USNO Astronomical Applications,
 * https://aa.usno.navy.mil/calculated/moon/phases?date=2026-11-01&nump=8
 * (fetched via timeanddate.com / USNO year table; live WebFetch timed out).
 *
 * Track + chip use civil dates at UTC noon with ±1.5-day new/full windows
 * ([SeasonalCalendar]); eight-phase names come from [grimoireMoonTrack]. Synodic
 * age can place new/full windows up to ~1 day before the published UTC instant.
 */
class MoonPhasePublishedDatesTest {
    @Test
    fun `published phases late Nov through Dec 2026`() {
        val cases = listOf(
            // USNO 2026-11-01 20:28 UTC Last Quarter — still waning gibbous at civil noon
            Triple(LocalDate.of(2026, 10, 31), "Waning Gibbous", 5),
            Triple(LocalDate.of(2026, 11, 1), "Last Quarter", 6),
            Triple(LocalDate.of(2026, 11, 2), "Last Quarter", 6),
            // USNO 2026-11-09 07:02 UTC New Moon
            Triple(LocalDate.of(2026, 11, 7), "Waning Crescent", 7),
            Triple(LocalDate.of(2026, 11, 8), "New Moon", 0),
            Triple(LocalDate.of(2026, 11, 9), "New Moon", 0),
            Triple(LocalDate.of(2026, 11, 10), "New Moon", 0),
            Triple(LocalDate.of(2026, 11, 11), "Waxing Crescent", 1),
            // USNO 2026-11-17 11:48 UTC First Quarter
            Triple(LocalDate.of(2026, 11, 16), "First Quarter", 2),
            Triple(LocalDate.of(2026, 11, 17), "First Quarter", 2),
            Triple(LocalDate.of(2026, 11, 18), "First Quarter", 2),
            // USNO 2026-11-24 14:53 UTC Full Moon — window begins civil 11-23
            Triple(LocalDate.of(2026, 11, 22), "Waxing Gibbous", 3),
            Triple(LocalDate.of(2026, 11, 23), "Full Moon", 4),
            Triple(LocalDate.of(2026, 11, 24), "Full Moon", 4),
            Triple(LocalDate.of(2026, 11, 25), "Full Moon", 4),
            Triple(LocalDate.of(2026, 11, 26), "Waning Gibbous", 5),
            // USNO 2026-12-01 06:08 UTC Last Quarter
            Triple(LocalDate.of(2026, 12, 1), "Last Quarter", 6),
            // USNO 2026-12-09 00:52 UTC New Moon
            Triple(LocalDate.of(2026, 12, 8), "New Moon", 0),
            Triple(LocalDate.of(2026, 12, 9), "New Moon", 0),
            Triple(LocalDate.of(2026, 12, 10), "New Moon", 0),
            // USNO 2026-12-17 05:42 UTC First Quarter
            Triple(LocalDate.of(2026, 12, 17), "First Quarter", 2),
            // USNO 2026-12-24 01:28 UTC Full Moon
            Triple(LocalDate.of(2026, 12, 22), "Waxing Gibbous", 3),
            Triple(LocalDate.of(2026, 12, 23), "Full Moon", 4),
            Triple(LocalDate.of(2026, 12, 24), "Full Moon", 4),
            Triple(LocalDate.of(2026, 12, 25), "Full Moon", 4),
            // USNO 2026-12-30 18:59 UTC Last Quarter
            Triple(LocalDate.of(2026, 12, 30), "Last Quarter", 6),
            Triple(LocalDate.of(2026, 12, 31), "Last Quarter", 6),
        )
        cases.forEach { (date, name, index) ->
            val track = grimoireMoonTrack(date)
            assertEquals(date.toString(), name, track.name)
            assertEquals(date.toString(), index, track.trackIndex)
        }
    }

    @Test
    fun `chip windows align with track on USNO new and full nights`() {
        val newMoonWindow = listOf(
            LocalDate.of(2026, 11, 8),
            LocalDate.of(2026, 11, 9),
            LocalDate.of(2026, 11, 10),
        )
        newMoonWindow.forEach { date ->
            assertEquals(date.toString(), SeasonalWindow.Moon("new"), SeasonalCalendar.seasonalWindowFor(date))
            assertEquals(date.toString(), 0, grimoireMoonTrack(date).trackIndex)
            assertEquals(date.toString(), "New Moon", grimoireMoonTrack(date).name)
        }

        val fullMoonWindow = listOf(
            LocalDate.of(2026, 11, 23),
            LocalDate.of(2026, 11, 24),
            LocalDate.of(2026, 11, 25),
        )
        fullMoonWindow.forEach { date ->
            assertEquals(date.toString(), SeasonalWindow.Moon("full"), SeasonalCalendar.seasonalWindowFor(date))
            assertEquals(date.toString(), 4, grimoireMoonTrack(date).trackIndex)
            assertEquals(date.toString(), "Full Moon", grimoireMoonTrack(date).name)
        }
    }
}
