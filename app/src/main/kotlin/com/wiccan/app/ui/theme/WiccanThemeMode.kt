package com.wiccan.app.ui.theme

/**
 * How the active [WiccanPalette] is chosen. Persisted via `ThemePreferences`;
 * the user's explicit choice always wins over anything automatic.
 */
enum class WiccanThemeMode {
    /** Default. Follows [SeasonalThemeResolver]'s Wheel-of-the-Year mapping. */
    AUTOMATIC_SEASONAL,

    /** Ignores the season; uses Near Black in system dark mode, Light/Ivory in system light mode. */
    FOLLOW_SYSTEM,

    /** Locked to a single palette regardless of season or system setting. */
    NEAR_BLACK,
    MIDNIGHT_BLUE,
    ROYAL_PLUM,
    DEEP_FOREST,
    WARM_EARTH,
    LIGHT_IVORY,
    ;

    /** The palette this mode locks to, or `null` if it depends on season/system state. */
    val lockedPalette: WiccanPalette?
        get() = when (this) {
            AUTOMATIC_SEASONAL, FOLLOW_SYSTEM -> null
            NEAR_BLACK -> WiccanPalette.NEAR_BLACK
            MIDNIGHT_BLUE -> WiccanPalette.MIDNIGHT_BLUE
            ROYAL_PLUM -> WiccanPalette.ROYAL_PLUM
            DEEP_FOREST -> WiccanPalette.DEEP_FOREST
            WARM_EARTH -> WiccanPalette.WARM_EARTH
            LIGHT_IVORY -> WiccanPalette.LIGHT_IVORY
        }

    /** Label for the Settings theme picker. */
    val displayName: String
        get() = when (this) {
            AUTOMATIC_SEASONAL -> "Automatic — Wheel of the Year"
            FOLLOW_SYSTEM -> "Follow System"
            else -> checkNotNull(lockedPalette).displayName
        }

    /** Subtitle for the Settings theme picker. */
    val description: String
        get() = when (this) {
            AUTOMATIC_SEASONAL -> "Recommended. Rolls gradually through all six palettes with the seasons."
            FOLLOW_SYSTEM -> "Near Black in dark mode, Light/Ivory in light mode."
            else -> checkNotNull(lockedPalette).moodDescription
        }

    companion object {
        val DEFAULT = AUTOMATIC_SEASONAL

        /** Parses a persisted name, falling back to [DEFAULT] for anything missing, invalid, or from a legacy schema. */
        fun fromPersistedName(name: String?): WiccanThemeMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
