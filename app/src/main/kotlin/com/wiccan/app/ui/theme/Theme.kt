package com.wiccan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wiccan.app.domain.model.WiccanThemeMode
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

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
    content: @Composable () -> Unit,
) {
    val today = rememberCurrentDate()
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

@Composable
private fun rememberCurrentDate(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                today = LocalDate.now()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = ZonedDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis())
            today = LocalDate.now()
        }
    }

    return today
}
