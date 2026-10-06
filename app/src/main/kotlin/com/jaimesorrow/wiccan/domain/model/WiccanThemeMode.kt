package com.jaimesorrow.wiccan.domain.model

/**
 * How the active Wiccan palette is chosen. Persisted via `ThemePreferences`;
 * the user's explicit choice always wins over anything automatic.
 */
enum class WiccanThemeMode {
    AUTOMATIC_SEASONAL,
    FOLLOW_SYSTEM,
    NEAR_BLACK,
    MIDNIGHT_BLUE,
    ROYAL_PLUM,
    DEEP_FOREST,
    WARM_EARTH,
    LIGHT_IVORY,
    ;

    companion object {
        val DEFAULT = AUTOMATIC_SEASONAL

        /** Parses a persisted name, falling back to [DEFAULT] for anything missing, invalid, or from a legacy schema. */
        fun fromPersistedName(name: String?): WiccanThemeMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
