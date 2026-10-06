package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentStatus
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import com.jaimesorrow.wiccan.ui.settings.ThemeModePicker
import com.jaimesorrow.wiccan.ui.today.TodayScreenContent
import com.jaimesorrow.wiccan.ui.today.TodayUiState
import kotlinx.coroutines.flow.emptyFlow

/**
 * Visual validation for the six-palette system. There is no Android
 * emulator/device in this sandbox to run instrumented screenshot tests
 * against, so this is deliberately the honest fallback: real `@Preview`
 * composables a developer opens in Android Studio's Split/Design view to
 * see all six palettes side by side (Today screen + the Settings picker),
 * rather than a fabricated screenshot. See `docs/STATUS.md` for this
 * caveat.
 */
private val SamplePreviewContent = Content(
    id = "preview-content",
    type = ContentType.SPELL,
    tier = ContentTier.FREE,
    tradition = "Eclectic",
    category = ContentCategory.PROTECTION,
    title = "A Ward for the Threshold",
    intro = "A short protective working for the entrance to your home.",
    body = "Light the candle at the doorway and trace the sigil in the air, " +
        "moving deosil three times while holding the intention of a safe " +
        "and settled home.",
    materials = listOf("White candle", "Rosemary", "A small dish of salt"),
    safetyNote = "Never leave a burning candle unattended.",
    tags = listOf("protection", "home"),
    status = ContentStatus.PUBLISHED,
)

@Composable
private fun TodayScreenPreview(mode: WiccanThemeMode) {
    WiccanTheme(themeMode = mode) {
        TodayScreenContent(
            state = TodayUiState(isLoading = false, content = SamplePreviewContent),
            effects = emptyFlow(),
            onEvent = {},
        )
    }
}

@Preview(name = "Today — Near Black", showBackground = true)
@Composable
private fun TodayScreenNearBlackPreview() = TodayScreenPreview(WiccanThemeMode.NEAR_BLACK)

@Preview(name = "Today — Midnight Blue", showBackground = true)
@Composable
private fun TodayScreenMidnightBluePreview() = TodayScreenPreview(WiccanThemeMode.MIDNIGHT_BLUE)

@Preview(name = "Today — Royal Plum", showBackground = true)
@Composable
private fun TodayScreenRoyalPlumPreview() = TodayScreenPreview(WiccanThemeMode.ROYAL_PLUM)

@Preview(name = "Today — Deep Forest", showBackground = true)
@Composable
private fun TodayScreenDeepForestPreview() = TodayScreenPreview(WiccanThemeMode.DEEP_FOREST)

@Preview(name = "Today — Warm Earth", showBackground = true)
@Composable
private fun TodayScreenWarmEarthPreview() = TodayScreenPreview(WiccanThemeMode.WARM_EARTH)

@Preview(name = "Today — Light Ivory", showBackground = true)
@Composable
private fun TodayScreenLightIvoryPreview() = TodayScreenPreview(WiccanThemeMode.LIGHT_IVORY)

@Composable
private fun ThemeModePickerPreview(mode: WiccanThemeMode) {
    WiccanTheme(themeMode = mode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            var selected by remember { mutableStateOf(mode) }
            ThemeModePicker(
                selected = selected,
                onSelected = { selected = it },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "Settings theme picker — Near Black", showBackground = true)
@Composable
private fun ThemeModePickerNearBlackPreview() = ThemeModePickerPreview(WiccanThemeMode.NEAR_BLACK)

@Preview(name = "Settings theme picker — Light Ivory", showBackground = true)
@Composable
private fun ThemeModePickerLightIvoryPreview() = ThemeModePickerPreview(WiccanThemeMode.LIGHT_IVORY)
