package com.wiccan.app.ui.theme

import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit

/**
 * Result of resolving the Wheel-of-the-Year palette for a given date.
 * [blendTarget]/[blendFraction] are only non-default in the week leading
 * up to the next sabbat, so a caller can cross-fade toward it instead of
 * snapping at midnight.
 */
data class SeasonalResolution(
    val dominant: WiccanPalette,
    val blendTarget: WiccanPalette? = null,
    val blendFraction: Float = 0f,
)

/**
 * Deterministic, offline mapping from a calendar date to a [WiccanPalette],
 * following the eight-spoke Wheel of the Year. This is purely a product
 * feature mapping dates to a color mood - no user-facing text derived from
 * it should make any historical claim about these observances.
 *
 * ## The mapping
 *
 * The eight sabbats sit on one continuous mood arc that starts and ends at
 * the year's darkest point (Yule) and peaks at its brightest (Litha),
 * mirrored on the way back down so the festival on each side of a solstice
 * shares a palette with its mirror:
 *
 * | Sabbat     | ~Date  | Palette       | Why |
 * |---|---|---|---|
 * | Yule       | Dec 21 | Near Black    | Longest night - the year's darkest, most minimal point. |
 * | Imbolc     | Feb 1  | Midnight Blue | First light returning, still cold and quiet - a clear sky before dawn. |
 * | Ostara     | Mar 20 | Deep Forest   | Spring equinox - new growth, herbs and earth waking. |
 * | Beltane    | May 1  | Royal Plum    | Fire festival - blossoming, vivid, mystical. |
 * | Litha      | Jun 21 | Light / Ivory | Summer solstice - the year's brightest, most daylit point. |
 * | Lughnasadh | Aug 1  | Warm Earth    | First harvest - grain fields, candlelight, ritual warmth. |
 * | Mabon      | Sep 22 | Royal Plum    | Autumn equinox - dusk, orchard/wine harvest; mirrors Beltane descending. |
 * | Samhain    | Oct 31 | Midnight Blue | The veil thins and the year turns inward; mirrors Imbolc descending. |
 *
 * Royal Plum and Midnight Blue each anchor two points (rising and falling
 * sides of the year) so all six palettes are used while the arc stays
 * symmetric around the solstices.
 *
 * ## Transitions
 *
 * A palette holds steady for most of its ~6-7 week span. In the final
 * [TRANSITION_WINDOW_DAYS] days before the next anchor, [resolve] reports a
 * rising [SeasonalResolution.blendFraction] toward [SeasonalResolution.blendTarget]
 * (see `rememberWiccanColorScheme`) instead of changing harshly at midnight.
 * On the anchor day itself the new palette becomes the pure dominant.
 */
object SeasonalThemeResolver {
    private const val TRANSITION_WINDOW_DAYS = 7L

    private data class Anchor(val monthDay: MonthDay, val palette: WiccanPalette)

    // Ordered by day-of-year (MonthDay compares month then day):
    // Imbolc, Ostara, Beltane, Litha, Lughnasadh, Mabon, Samhain, Yule.
    private val anchors = listOf(
        Anchor(MonthDay.of(2, 1), WiccanPalette.MIDNIGHT_BLUE),
        Anchor(MonthDay.of(3, 20), WiccanPalette.DEEP_FOREST),
        Anchor(MonthDay.of(5, 1), WiccanPalette.ROYAL_PLUM),
        Anchor(MonthDay.of(6, 21), WiccanPalette.LIGHT_IVORY),
        Anchor(MonthDay.of(8, 1), WiccanPalette.WARM_EARTH),
        Anchor(MonthDay.of(9, 22), WiccanPalette.ROYAL_PLUM),
        Anchor(MonthDay.of(10, 31), WiccanPalette.MIDNIGHT_BLUE),
        Anchor(MonthDay.of(12, 21), WiccanPalette.NEAR_BLACK),
    )

    fun resolve(date: LocalDate): SeasonalResolution {
        val monthDay = MonthDay.from(date)

        // The last anchor at/before `date`'s month-day, wrapping to Yule
        // (the final anchor) for dates before the year's first anchor.
        val currentAnchor = anchors.lastOrNull { it.monthDay <= monthDay } ?: anchors.last()
        val currentIndex = anchors.indexOf(currentAnchor)
        val nextAnchor = anchors[(currentIndex + 1) % anchors.size]

        val currentAnchorDate = latestOccurrenceOnOrBefore(date, currentAnchor.monthDay)
        val nextAnchorDate = earliestOccurrenceAfter(currentAnchorDate, nextAnchor.monthDay)

        val daysUntilNext = ChronoUnit.DAYS.between(date, nextAnchorDate)

        return if (daysUntilNext in 0 until TRANSITION_WINDOW_DAYS && currentAnchor.palette != nextAnchor.palette) {
            val fraction = (TRANSITION_WINDOW_DAYS - daysUntilNext).toFloat() / TRANSITION_WINDOW_DAYS.toFloat()
            SeasonalResolution(
                dominant = currentAnchor.palette,
                blendTarget = nextAnchor.palette,
                blendFraction = fraction,
            )
        } else {
            SeasonalResolution(dominant = currentAnchor.palette)
        }
    }

    /** The latest date `<= date` whose month/day matches [monthDay]. */
    private fun latestOccurrenceOnOrBefore(date: LocalDate, monthDay: MonthDay): LocalDate {
        val thisYear = monthDay.atYear(date.year)
        return if (!thisYear.isAfter(date)) thisYear else monthDay.atYear(date.year - 1)
    }

    /** The earliest date `> after` whose month/day matches [monthDay]. */
    private fun earliestOccurrenceAfter(after: LocalDate, monthDay: MonthDay): LocalDate {
        val thisYear = monthDay.atYear(after.year)
        return if (thisYear.isAfter(after)) thisYear else monthDay.atYear(after.year + 1)
    }
}
