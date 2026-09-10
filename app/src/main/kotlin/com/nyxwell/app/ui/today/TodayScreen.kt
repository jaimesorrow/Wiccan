package com.nyxwell.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.nyxwell.app.core.error.toUserMessage
import com.nyxwell.app.domain.model.Content
import com.nyxwell.app.ui.today.components.ContentTypePill
import com.nyxwell.app.ui.today.components.CrescentGlyph
import com.nyxwell.app.ui.today.components.HairlineDivider
import com.nyxwell.app.ui.today.components.MaterialsCard
import com.nyxwell.app.ui.today.components.SafetyNote
import com.nyxwell.app.ui.today.components.TodaySkeleton
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
                    val result = snackbarHostState.showSnackbar(
                        message = effect.error.toUserMessage(),
                        actionLabel = "Retry",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
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
                state.content != null -> LoadedContent(state.content)
                else -> {
                    // Post-error, cache-empty: keep the ground quiet — the snackbar carries the message.
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(content: Content) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 24.dp, vertical = 32.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Decorative crescent, centered above the header row.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CrescentGlyph()
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ContentTypePill(content.type)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = content.tradition,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
            )
        }

        Text(
            text = content.title,
            style = MaterialTheme.typography.displayLarge,
            color = colors.onBackground,
        )

        if (content.intro.isNotBlank()) {
            Text(
                text = content.intro,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }

        HairlineDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = content.body,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onBackground,
        )

        content.materials?.takeIf { it.isNotEmpty() }?.let { materials ->
            Spacer(modifier = Modifier.height(8.dp))
            MaterialsCard(materials = materials)
        }

        content.safetyNote?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(modifier = Modifier.height(8.dp))
            SafetyNote(note = note)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
