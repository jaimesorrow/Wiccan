package com.wiccan.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.error.toUserMessage
import com.wiccan.app.ui.content.ContentDetailView
import com.wiccan.app.ui.today.components.TodaySkeleton
import kotlinx.coroutines.flow.Flow

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TodayScreenContent(
        state = state,
        effects = viewModel.effects,
        onEvent = viewModel::onEvent,
        modifier = modifier,
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
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is TodayUiEffect.ShowSnackbar -> {
                    val canRetry = effect.error !is AppError.Unauthorized
                    val result = snackbarHostState.showSnackbar(
                        message = effect.error.toUserMessage(),
                        actionLabel = if (canRetry) "Retry" else null,
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
                state.newLessonPending -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Today's new lesson is being prepared",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "There isn't a new reviewed lesson ready for you today. You can revisit your archive separately.",
                        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Button(onClick = { onEvent(TodayUiEvent.Retry) }) {
                        Text("Check again")
                    }
                }
                state.content != null -> ContentDetailView(state.content)
                else -> {
                    // Post-error, cache-empty: keep the ground quiet — the snackbar carries the message.
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
            }
        }
    }
}
