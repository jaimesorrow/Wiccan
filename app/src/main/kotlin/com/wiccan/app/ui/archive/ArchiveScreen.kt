package com.wiccan.app.ui.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The user's delivery history, newest first — "Book of Shadows" Timeline.
 * Tapping an entry navigates to the `archive_detail/{contentId}` route.
 */
@Composable
fun ArchiveScreen(
    modifier: Modifier = Modifier,
    viewModel: ArchiveViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ArchiveUiEffect.NavigateToDetail -> onNavigateToDetail(effect.contentId)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Box(modifier = Modifier.padding(insets).fillMaxSize()) {
            when {
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
