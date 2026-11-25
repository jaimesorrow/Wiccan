package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.runtime.Composable
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode

/**
 * Midnight Grimoire for every screen. [themeMode] is still accepted so existing
 * call sites compile; it no longer changes colors. Lesson pages that sit on
 * parchment read [WiccanPaletteColors.Parchment] inside this dark stage.
 */
@Composable
fun WiccanTheme(
    @Suppress("UNUSED_PARAMETER") themeMode: WiccanThemeMode = WiccanThemeMode.DEFAULT,
    content: @Composable () -> Unit,
) {
    MidnightGrimoireTheme(content)
}
