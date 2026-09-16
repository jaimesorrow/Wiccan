package com.wiccan.app.ui.theme

import java.time.LocalDate

/** User-selectable visual identity for Wiccan. */
enum class WiccanThemeMode {
    AUTOMATIC,
    NEAR_BLACK,
    MIDNIGHT_BLUE,
    ROYAL_PLUM,
    DEEP_FOREST,
    WARM_EARTH,
    LIGHT_IVORY,
    FOLLOW_SYSTEM,
}

internal enum class WiccanPalette {
    NEAR_BLACK,
    MIDNIGHT_BLUE,
    ROYAL_PLUM,
    DEEP_FOREST,
    WARM_EARTH,
    LIGHT_IVORY,
}

/**
 * Deterministic, offline seasonal resolver. Boundaries intentionally use the
 * eight Wheel-of-the-Year anchor dates as product theming markers; they are
 * not presented as historical claims about Wicca.
 */
internal object SeasonalThemeResolver {
    fun resolve(date: LocalDate): WiccanPalette {
        val key = date.monthValue * 100 + date.dayOfMonth
        return when (key) {
            in 1221..1231, in 101..131 -> WiccanPalette.NEAR_BLACK // Yule -> Imbolc
            in 201..319 -> WiccanPalette.LIGHT_IVORY // Imbolc -> Ostara
            in 320..430 -> WiccanPalette.DEEP_FOREST // Ostara -> Beltane
            in 501..620 -> WiccanPalette.ROYAL_PLUM // Beltane -> Litha
            in 621..731 -> WiccanPalette.WARM_EARTH // Litha -> Lughnasadh
            in 801..921 -> WiccanPalette.WARM_EARTH // Lughnasadh -> Mabon
            in 922..1030 -> WiccanPalette.MIDNIGHT_BLUE // Mabon -> Samhain
            else -> WiccanPalette.NEAR_BLACK // Samhain -> Yule
        }
    }
}
