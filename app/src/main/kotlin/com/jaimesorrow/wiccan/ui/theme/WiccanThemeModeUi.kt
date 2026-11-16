package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import com.wiccan.app.R

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
val WiccanPalette.displayName: String
    @Composable get() = stringResource(
        when (this) {
            WiccanPalette.NEAR_BLACK -> R.string.palette_near_black
            WiccanPalette.MIDNIGHT_BLUE -> R.string.palette_midnight
            WiccanPalette.ROYAL_PLUM -> R.string.palette_plum
            WiccanPalette.DEEP_FOREST -> R.string.palette_forest
            WiccanPalette.WARM_EARTH -> R.string.palette_earth
            WiccanPalette.LIGHT_IVORY -> R.string.palette_ivory
        },
    )

/** Subtitle for a locked palette in the Settings theme picker. */
val WiccanPalette.moodDescription: String
    @Composable get() = stringResource(
        when (this) {
            WiccanPalette.NEAR_BLACK -> R.string.palette_near_black_mood
            WiccanPalette.MIDNIGHT_BLUE -> R.string.palette_midnight_mood
            WiccanPalette.ROYAL_PLUM -> R.string.palette_plum_mood
            WiccanPalette.DEEP_FOREST -> R.string.palette_forest_mood
            WiccanPalette.WARM_EARTH -> R.string.palette_earth_mood
            WiccanPalette.LIGHT_IVORY -> R.string.palette_ivory_mood
        },
    )

/** Label for the Settings theme picker. */
val WiccanThemeMode.displayName: String
    @Composable get() = when (this) {
        WiccanThemeMode.AUTOMATIC_SEASONAL -> stringResource(R.string.theme_mode_wheel)
        WiccanThemeMode.FOLLOW_SYSTEM -> stringResource(R.string.theme_mode_system)
        else -> checkNotNull(lockedPalette).displayName
    }

/** Subtitle for the Settings theme picker. */
val WiccanThemeMode.description: String
    @Composable get() = when (this) {
        WiccanThemeMode.AUTOMATIC_SEASONAL -> stringResource(R.string.theme_mode_wheel_hint)
        WiccanThemeMode.FOLLOW_SYSTEM -> stringResource(R.string.theme_mode_system_hint)
        else -> checkNotNull(lockedPalette).moodDescription
    }
