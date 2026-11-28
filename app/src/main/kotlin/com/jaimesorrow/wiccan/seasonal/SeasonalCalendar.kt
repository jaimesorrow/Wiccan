package com.jaimesorrow.wiccan.seasonal

import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

/**
 * Civil-date sabbat and new/full-moon windows used by the Today chip and by
 * `functions/src/seasonalCalendar.ts` for daily lesson preference.
 *
 * Both sides sample the user's local calendar date at **UTC noon**, use the
 * same mean synodic-month age with a ±1.5-day new/full window, and use the
 * same fixed product sabbat anchors (not the solar-longitude dates in
 * [com.jaimesorrow.wiccan.sky.CelestialCalculator]). Keep this file and the
 * TypeScript module in lockstep; [SeasonalCalendarTest] shares sample dates
 * with `functions/src/__tests__/seasonalCalendar.test.ts`.
 */
internal object SeasonalCalendar {
    const val SYNODIC_MONTH_DAYS = 29.530588853

    /** `Date.UTC(2000, 0, 6, 18, 14)` — 2000-01-06 18:14 UTC. */
    const val KNOWN_NEW_MOON_MS = 947_182_440_000L
    const val DAY_MS = 86_400_000L
    const val PHASE_WINDOW_DAYS = 1.5

    val SABBATS: Map<String, String> = mapOf(
        "02-01" to "imbolc",
        "03-20" to "ostara",
        "05-01" to "beltane",
        "06-21" to "litha",
        "08-01" to "lughnasadh",
        "09-22" to "mabon",
        "10-31" to "samhain",
        "12-21" to "yule",
    )

    fun moonPhase(date: LocalDate): String {
        val cycles = (utcNoonMillis(date) - KNOWN_NEW_MOON_MS) / DAY_MS.toDouble() / SYNODIC_MONTH_DAYS
        val age = ((cycles % 1 + 1) % 1) * SYNODIC_MONTH_DAYS
        return when {
            age < PHASE_WINDOW_DAYS || age > SYNODIC_MONTH_DAYS - PHASE_WINDOW_DAYS -> "new"
            abs(age - SYNODIC_MONTH_DAYS / 2) < PHASE_WINDOW_DAYS -> "full"
            age < SYNODIC_MONTH_DAYS / 2 -> "waxing"
            else -> "waning"
        }
    }

    /** Product-calendar sabbat anchor for the supplied date; it makes no historical claim. */
    fun sabbatFor(date: LocalDate): String? = SABBATS[date.toString().substring(5)]

    fun seasonalWindowFor(date: LocalDate): SeasonalWindow? {
        val sabbat = sabbatFor(date)
        if (sabbat != null) return SeasonalWindow.Sabbat(sabbat)
        val phase = moonPhase(date)
        return if (phase == "new" || phase == "full") SeasonalWindow.Moon(phase) else null
    }

    private fun utcNoonMillis(date: LocalDate): Long =
        date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
}

internal sealed class SeasonalWindow {
    data class Sabbat(val id: String) : SeasonalWindow()
    data class Moon(val phase: String) : SeasonalWindow()
}
