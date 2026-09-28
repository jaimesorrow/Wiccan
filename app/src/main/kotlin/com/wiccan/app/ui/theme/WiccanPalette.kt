package com.wiccan.app.ui.theme

/**
 * The six approved expressions of the Wiccan visual identity. Every
 * palette shares the same typography, spacing, component geometry and
 * information architecture (see [WiccanTheme]) - only color changes.
 *
 * [isLight] selects a Material3 `lightColorScheme`/`darkColorScheme`
 * builder in [toColorScheme]; every other palette is dark-first by design.
 */
enum class WiccanPalette(
    val displayName: String,
    val moodDescription: String,
    val isLight: Boolean,
) {
    NEAR_BLACK(
        displayName = "Near Black",
        moodDescription = "Modern & minimal",
        isLight = false,
    ),
    MIDNIGHT_BLUE(
        displayName = "Midnight Blue",
        moodDescription = "Celestial & serene",
        isLight = false,
    ),
    ROYAL_PLUM(
        displayName = "Royal Plum",
        moodDescription = "Mystical & elegant",
        isLight = false,
    ),
    DEEP_FOREST(
        displayName = "Deep Forest",
        moodDescription = "Earthy & grounded",
        isLight = false,
    ),
    WARM_EARTH(
        displayName = "Warm Earth",
        moodDescription = "Ritual & candlelight",
        isLight = false,
    ),
    LIGHT_IVORY(
        displayName = "Light / Ivory",
        moodDescription = "Airy & peaceful",
        isLight = true,
    ),
}
