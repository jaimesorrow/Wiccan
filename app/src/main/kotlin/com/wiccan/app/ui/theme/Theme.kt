package com.wiccan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import java.time.LocalDate

/**
 * Wraps [content] in the active Wiccan palette (see [WiccanThemeMode] and
 * [SeasonalThemeResolver]) plus the shared [WiccanTypography]. Every screen
 * should read colors from `MaterialTheme.colorScheme.*` rather than a
 * hardcoded value, so it automatically follows whichever of the six
 * palettes - and whichever seasonal blend - is currently active.
 */
@Composable
fun WiccanTheme(
    themeMode: WiccanThemeMode = WiccanThemeMode.DEFAULT,
    today: LocalDate = LocalDate.now(),
    content: @Composable () -> Unit,
) {
    val colorScheme = rememberWiccanColorScheme(
        themeMode = themeMode,
        isSystemInDarkTheme = isSystemInDarkTheme(),
        today = today,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WiccanTypography,
        content = content,
    )
}
