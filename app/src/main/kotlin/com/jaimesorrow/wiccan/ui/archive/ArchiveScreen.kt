package com.jaimesorrow.wiccan.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.HistoryEntry

/**
 * The user's delivered content, either as a chronological Timeline or grouped
 * into a collapsible category Library. Tapping an entry navigates to the
 * `archive_detail/{contentId}` route.
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
                else -> Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        ArchiveTabRow(
                            selectedTab = state.selectedTab,
                            onTabSelected = { viewModel.onEvent(ArchiveUiEvent.TabSelected(it)) },
                        )
                        when (state.selectedTab) {
                            ArchiveTab.TIMELINE -> TimelineList(
                                entries = state.entries,
                                onEntryClicked = { viewModel.onEvent(ArchiveUiEvent.EntryClicked(it)) },
                            )
                            ArchiveTab.LIBRARY -> LibraryList(
                                entries = state.entries,
                                collapsedCategories = state.collapsedCategories,
                                onEntryClicked = { viewModel.onEvent(ArchiveUiEvent.EntryClicked(it)) },
                                onCategoryToggled = { viewModel.onEvent(ArchiveUiEvent.CategoryToggled(it)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveTabRow(selectedTab: ArchiveTab, onTabSelected: (ArchiveTab) -> Unit) {
    TabRow(selectedTabIndex = selectedTab.ordinal) {
        Tab(
            selected = selectedTab == ArchiveTab.TIMELINE,
            onClick = { onTabSelected(ArchiveTab.TIMELINE) },
            text = { Text("Timeline") },
        )
        Tab(
            selected = selectedTab == ArchiveTab.LIBRARY,
            onClick = { onTabSelected(ArchiveTab.LIBRARY) },
            text = { Text("Library") },
        )
    }
}

@Composable
private fun TimelineList(entries: List<HistoryEntry>, onEntryClicked: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(entries, key = { "${it.contentId}-${it.deliveredDate}" }) { entry ->
            ArchiveEntryRow(entry = entry, onClick = { onEntryClicked(entry.contentId) })
        }
    }
}

@Composable
private fun LibraryList(
    entries: List<HistoryEntry>,
    collapsedCategories: Set<ContentCategory>,
    onEntryClicked: (String) -> Unit,
    onCategoryToggled: (ContentCategory) -> Unit,
) {
    val groups = entries.groupBy { it.category }.toList().sortedBy { it.first.ordinal }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { (category, categoryEntries) ->
            val isCollapsed = category in collapsedCategories
            item(key = "header-${category.name}") {
                CategoryHeader(
                    category = category,
                    count = categoryEntries.size,
                    isCollapsed = isCollapsed,
                    onClick = { onCategoryToggled(category) },
                )
            }
            if (!isCollapsed) {
                items(categoryEntries, key = { "${it.contentId}-${it.deliveredDate}" }) { entry ->
                    ArchiveEntryRow(entry = entry, onClick = { onEntryClicked(entry.contentId) })
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: ContentCategory, count: Int, isCollapsed: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${category.displayName()} ($count)",
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (isCollapsed) "▸" else "▾",
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurfaceVariant,
        )
    }
}

/** Human-readable label for a [ContentCategory], for the Library grouping headers. */
private fun ContentCategory.displayName(): String = when (this) {
    ContentCategory.PROTECTION -> "Protection"
    ContentCategory.LOVE_AND_RELATIONSHIPS -> "Love & Relationships"
    ContentCategory.PROSPERITY_AND_ABUNDANCE -> "Prosperity & Abundance"
    ContentCategory.HEALING_AND_WELLNESS -> "Healing & Wellness"
    ContentCategory.DIVINATION_AND_CLARITY -> "Divination & Clarity"
    ContentCategory.BANISHING_AND_RELEASE -> "Banishing & Release"
    ContentCategory.HOME_AND_HEARTH -> "Home & Hearth"
    ContentCategory.MOON_AND_SEASONAL -> "Moon & Seasonal"
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
