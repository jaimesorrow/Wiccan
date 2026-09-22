package com.wiccan.app.ui.theme

import com.wiccan.app.domain.model.WiccanThemeMode

/** The palette this mode locks to, or `null` if it depends on season/system state. */
val WiccanThemeMode.lockedPalette: WiccanPalette?
    get() = when (this) {
        WiccanThemeMode.AUTOMATIC_SEASONAL, WiccanThemeMode.FOLLOW_SYSTEM -> null
        WiccanThemeMode.NEAR_BLACK -> WiccanPalette.NEAR_BLACK
        WiccanThemeMode.MIDNIGHT_BLUE -> WiccanPalette.MIDNIGHT_BLUE
        WiccanThemeMode.ROYAL_PLUM -> WiccanPalette.ROYAL_PLUM
        WiccanThemeMode.DEEP_FOREST -> WiccanPalette.DEEP_FOREST
        WiccanThemeMode.WARM_EARTH -> WiccanPalette.WARM_EARTH
        WiccanThemeMode.LIGHT_IVORY -> WiccanPalette.LIGHT_IVORY
    }

/** Label for the Settings theme picker. */
val WiccanThemeMode.displayName: String
    get() = when (this) {
        WiccanThemeMode.AUTOMATIC_SEASONAL -> "Automatic — Wheel of the Year"
        WiccanThemeMode.FOLLOW_SYSTEM -> "Follow System"
        else -> checkNotNull(lockedPalette).displayName
    }

/** Subtitle for the Settings theme picker. */
val WiccanThemeMode.description: String
    get() = when (this) {
        WiccanThemeMode.AUTOMATIC_SEASONAL -> "Recommended. Changes palettes with the seasons."
        WiccanThemeMode.FOLLOW_SYSTEM -> "Near Black in dark mode, Light/Ivory in light mode."
        else -> checkNotNull(lockedPalette).moodDescription
    }
