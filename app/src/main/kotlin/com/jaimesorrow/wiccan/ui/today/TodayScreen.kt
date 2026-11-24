package com.jaimesorrow.wiccan.ui.today

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanEmblem
import com.jaimesorrow.wiccan.ui.brand.wiccanPageWash
import com.jaimesorrow.wiccan.ui.content.ContentDetailView
import com.jaimesorrow.wiccan.ui.content.DailyGrimoirePage
import com.jaimesorrow.wiccan.ui.today.components.TodaySkeleton
import com.wiccan.app.R
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenGrimoire: () -> Unit = {},
    onOpenToday: (() -> Unit)? = null,
    onOpenPaywall: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // A retained screen must ask the server again after a local-date or timezone change.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(TodayUiEvent.Resumed) }
    LaunchedEffect(viewModel) {
        while (true) {
            val now = ZonedDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1))
            viewModel.onEvent(TodayUiEvent.Retry)
        }
    }
    TodayScreenContent(
        state = state,
        effects = viewModel.effects,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        onOpenGrimoire = onOpenGrimoire,
        onOpenToday = onOpenToday,
        onOpenPaywall = onOpenPaywall,
        onBack = onBack,
    )
}

/**
 * State-hoisted body — kept internal so previews and (later) instrumentation
 * tests can drive it without a live ViewModel.
 */
@Composable
internal fun TodayScreenContent(
    state: TodayUiState,
    effects: Flow<TodayUiEffect>,
    onEvent: (TodayUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenGrimoire: () -> Unit = {},
    onOpenToday: (() -> Unit)? = null,
    onOpenPaywall: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val retryLabel = stringResource(R.string.today_retry)
    // Hoisted (and saveable) so pressing "Check again" / "Try again" doesn't
    // reset the panel to the top on short screens or with large text.
    val emptyScrollState = rememberScrollState()
    val errorScrollState = rememberScrollState()

    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is TodayUiEffect.ShowSnackbar -> {
                    val canRetry = effect.error !is AppError.Unauthorized
                    val result = snackbarHostState.showSnackbar(
                        message = effect.error.toUserMessage(resources),
                        actionLabel = if (canRetry) retryLabel else null,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                    if (canRetry && result == SnackbarResult.ActionPerformed) {
                        onEvent(TodayUiEvent.Retry)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        Column(modifier = Modifier.padding(insets).fillMaxSize()) {
            onBack?.let { back ->
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    TextButton(onClick = back) { Text(stringResource(R.string.daily_back)) }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth().wiccanPageWash()) {
                when {
                    onOpenToday != null -> SpellbookHome(
                        content = if (state.lessonLocked) null else state.content,
                        lessonLocked = state.lessonLocked,
                        onOpenToday = onOpenToday,
                        onOpenPaywall = onOpenPaywall,
                        onOpenGrimoire = onOpenGrimoire,
                    )
                    // A retry from the exhausted or error panel keeps that panel on
                    // screen (and its scroll position) instead of swapping in the skeleton.
                    state.isLoading && state.content == null && !state.poolExhausted && !state.loadFailed ->
                        TodaySkeleton()
                    // Centered when it fits; scrollable on short viewports or with
                    // enlarged text so the actions below always stay reachable.
                    state.poolExhausted -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier
                                .testTag("today-empty")
                                .verticalScroll(emptyScrollState)
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            WiccanEmblem(
                                modifier = Modifier.testTag("today-empty-logo"),
                                size = 48.dp,
                                contentDescription = stringResource(R.string.today_logo_description),
                            )
                            WiccanBotanicalAccent(width = 116.dp)
                            Text(
                                text = stringResource(R.string.today_empty_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = stringResource(R.string.today_empty_body),
                                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Button(onClick = onOpenGrimoire) { Text(stringResource(R.string.today_open_grimoire)) }
                            TextButton(
                                onClick = { onEvent(TodayUiEvent.Retry) },
                                enabled = !state.isLoading,
                                modifier = Modifier.testTag("today-check-again"),
                            ) {
                                Text(stringResource(R.string.today_look_again))
                            }
                        }
                    }
                    state.lessonLocked -> LessonLock(onOpenPaywall)
                    state.content != null -> DailyGrimoirePage(
                        content = state.content,
                        onOpenGrimoire = onOpenGrimoire,
                    )
                    // Post-error, cache-empty: never leave a blank screen — always offer a way forward.
                    state.loadFailed -> TodayErrorPanel(
                        onEvent = onEvent,
                        onOpenGrimoire = onOpenGrimoire,
                        scrollState = errorScrollState,
                        retrying = state.isLoading,
                    )
                    else -> {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayErrorPanel(
    onEvent: (TodayUiEvent) -> Unit,
    onOpenGrimoire: () -> Unit,
    scrollState: ScrollState,
    retrying: Boolean,
) {
    Box(
        modifier = Modifier.fillMaxSize().testTag("today-error"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WiccanEmblem(
                size = 48.dp,
                contentDescription = stringResource(R.string.today_logo_description),
            )
            WiccanBotanicalAccent(width = 116.dp)
            Text(
                text = stringResource(R.string.today_error_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.today_error_body),
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Button(
                onClick = { onEvent(TodayUiEvent.Retry) },
                enabled = !retrying,
                modifier = Modifier.testTag("today-retry"),
            ) { Text(stringResource(R.string.today_retry)) }
            TextButton(onClick = onOpenGrimoire) { Text(stringResource(R.string.today_open_grimoire)) }
        }
    }
}

@Composable
private fun LessonLock(onOpenPaywall: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_locked_lesson_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.home_locked_lesson_body),
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Button(onClick = onOpenPaywall, modifier = Modifier.testTag("today-unlock")) {
            Text(stringResource(R.string.home_unlock_lesson))
        }
    }
}
