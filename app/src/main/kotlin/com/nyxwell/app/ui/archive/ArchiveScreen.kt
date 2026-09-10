package com.nyxwell.app.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nyxwell.app.ui.content.ContentDetailView

/**
 * The user's delivery history, newest first — "Book of Shadows". Tapping
 * an entry opens its full content inline (no navigation graph exists
 * yet; that arrives once more screens do).
 */
@Composable
fun ArchiveScreen(modifier: Modifier = Modifier, viewModel: ArchiveViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ArchiveUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.error.toUserMessage())
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        Box(modifier = Modifier.padding(insets).fillMaxSize()) {
            val selected = state.selectedContent
            when {
                selected != null -> ArchiveDetail(
                    content = selected,
                    onBack = { viewModel.onEvent(ArchiveUiEvent.DismissDetail) },
                )
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.entries.isEmpty() -> EmptyArchive()
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.entries, key = { "${it.contentId}-${it.deliveredDate}" }) { entry ->
                        ArchiveEntryRow(
                            entry = entry,
                            onClick = { viewModel.onEvent(ArchiveUiEvent.EntryClicked(entry.contentId)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyArchive() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "Your Book of Shadows is empty — your first entry unlocks today",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ArchiveDetail(content: Content, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
            Text("← Back")
        }
        ContentDetailView(content = content, modifier = Modifier.fillMaxSize())
    }
}
