package com.jaimesorrow.wiccan.seasonal

import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

/**
 * Civil-date sabbat and new/full-moon windows used by the Today chip, the
 * lesson-page moon track, and `functions/src/seasonalCalendar.ts` for daily
 * lesson preference.
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

    private val WAXING_TRACK = listOf("Waxing Crescent", "First Quarter", "Waxing Gibbous")
    private val WANING_TRACK = listOf("Waning Gibbous", "Last Quarter", "Waning Crescent")

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

    fun moonAgeDays(date: LocalDate): Double {
        val cycles = (utcNoonMillis(date) - KNOWN_NEW_MOON_MS) / DAY_MS.toDouble() / SYNODIC_MONTH_DAYS
        return ((cycles % 1 + 1) % 1) * SYNODIC_MONTH_DAYS
    }

    fun moonPhase(date: LocalDate): String = phaseFromAge(moonAgeDays(date))

    /**
     * Eight-phase track derived from the same civil-date age as [moonPhase],
     * so the lesson-page glyphs cannot show a waxing/waning phase on a day
     * the chip calls new or full.
     */
    fun moonTrack(date: LocalDate): CivilMoonTrack {
        val age = moonAgeDays(date)
        val half = SYNODIC_MONTH_DAYS / 2
        return when (phaseFromAge(age)) {
            "new" -> CivilMoonTrack(name = "New Moon", trackIndex = 0)
            "full" -> CivilMoonTrack(name = "Full Moon", trackIndex = 4)
            "waxing" -> {
                val band = intermediateBand(age - PHASE_WINDOW_DAYS, half - 2 * PHASE_WINDOW_DAYS)
                CivilMoonTrack(name = WAXING_TRACK[band], trackIndex = 1 + band)
            }
            else -> {
                val band = intermediateBand(age - (half + PHASE_WINDOW_DAYS), half - 2 * PHASE_WINDOW_DAYS)
                CivilMoonTrack(name = WANING_TRACK[band], trackIndex = 5 + band)
            }
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

    private fun phaseFromAge(age: Double): String = when {
        age < PHASE_WINDOW_DAYS || age > SYNODIC_MONTH_DAYS - PHASE_WINDOW_DAYS -> "new"
        abs(age - SYNODIC_MONTH_DAYS / 2) < PHASE_WINDOW_DAYS -> "full"
        age < SYNODIC_MONTH_DAYS / 2 -> "waxing"
        else -> "waning"
    }

    private fun intermediateBand(offset: Double, span: Double): Int {
        val t = (offset / span).coerceIn(0.0, 0.999999)
        return (t * 3).toInt()
    }

    private fun utcNoonMillis(date: LocalDate): Long =
        date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
}

internal data class CivilMoonTrack(
    val name: String,
    val trackIndex: Int,
)

/** Moon row + lesson chip: one civil-date track so Home, Archive, Sign-in, and Today never disagree. */
internal fun grimoireMoonTrack(date: LocalDate): CivilMoonTrack = SeasonalCalendar.moonTrack(date)

internal sealed class SeasonalWindow {
    data class Sabbat(val id: String) : SeasonalWindow()
    data class Moon(val phase: String) : SeasonalWindow()
}
