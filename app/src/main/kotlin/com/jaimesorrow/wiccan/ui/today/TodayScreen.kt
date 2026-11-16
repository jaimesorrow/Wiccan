package com.jaimesorrow.wiccan.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.jaimesorrow.wiccan.ui.brand.WiccanEmblem
import com.jaimesorrow.wiccan.ui.content.ContentDetailView
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
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val retryLabel = stringResource(R.string.today_retry)

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
        Box(modifier = Modifier.padding(insets).fillMaxSize()) {
            when {
                state.isLoading && state.content == null -> TodaySkeleton()
                // Centered when it fits; scrollable on short viewports or with
                // enlarged text so the actions below always stay reachable.
                state.poolExhausted -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        WiccanEmblem(
                            modifier = Modifier.testTag("today-empty-logo"),
                            size = 48.dp,
                            contentDescription = stringResource(R.string.today_logo_description),
                        )
                        Spacer(modifier = Modifier.height(20.dp))
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
                        TextButton(onClick = { onEvent(TodayUiEvent.Retry) }) {
                            Text(stringResource(R.string.today_look_again))
                        }
                    }
                }
                state.content != null -> ContentDetailView(
                    content = state.content,
                    isDailyLesson = true,
                    onOpenGrimoire = onOpenGrimoire,
                )
                else -> {
                    // Post-error, cache-empty: keep the ground quiet — the snackbar carries the message.
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
            }
        }
    }
}
