package com.jaimesorrow.wiccan.ui.theme

/**
 * The six approved expressions of the Wiccan visual identity. Every
 * palette shares the same typography, spacing, component geometry and
 * information architecture (see [WiccanTheme]) - only color changes.
 *
 * [isLight] selects a Material3 `lightColorScheme`/`darkColorScheme`
 * builder in [toColorScheme]; every other palette is dark-first by design.
 * Display names and mood lines live in `strings.xml`.
 */
enum class WiccanPalette(
    val isLight: Boolean,
) {
    NEAR_BLACK(isLight = false),
    MIDNIGHT_BLUE(isLight = false),
    ROYAL_PLUM(isLight = false),
    DEEP_FOREST(isLight = false),
    WARM_EARTH(isLight = false),
    LIGHT_IVORY(isLight = true),
}
