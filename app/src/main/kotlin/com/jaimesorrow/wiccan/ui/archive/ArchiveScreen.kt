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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import com.jaimesorrow.wiccan.ui.brand.WiccanEmblem
import com.wiccan.app.R

/**
 * The user's delivered content, either as a chronological Timeline or grouped
 * into collapsible categories in Grimoire. Tapping an entry navigates to the
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

    ArchiveScreenContent(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
internal fun ArchiveScreenContent(
    state: ArchiveUiState,
    onEvent: (ArchiveUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Column(modifier = Modifier.padding(insets).fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                WiccanEmblem(
                    modifier = Modifier.testTag("grimoire-logo"),
                    size = 44.dp,
                    contentDescription = stringResource(R.string.grimoire_logo_description),
                )
            }
            Text(
                text = stringResource(R.string.grimoire_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Text(
                text = stringResource(R.string.grimoire_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            val loadingDescription = stringResource(R.string.grimoire_loading)
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize().semantics { contentDescription = loadingDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.entries.isEmpty() -> EmptyArchive()
                else -> Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        ArchiveTabRow(
                            selectedTab = state.selectedTab,
                            onTabSelected = { onEvent(ArchiveUiEvent.TabSelected(it)) },
                        )
                        when (state.selectedTab) {
                            ArchiveTab.TIMELINE -> TimelineList(
                                entries = state.entries,
                                onEntryClicked = { onEvent(ArchiveUiEvent.EntryClicked(it)) },
                            )
                            ArchiveTab.LIBRARY -> LibraryList(
                                entries = state.entries,
                                collapsedCategories = state.collapsedCategories,
                                onEntryClicked = { onEvent(ArchiveUiEvent.EntryClicked(it)) },
                                onCategoryToggled = { onEvent(ArchiveUiEvent.CategoryToggled(it)) },
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
            text = { Text(stringResource(R.string.grimoire_timeline)) },
        )
        Tab(
            selected = selectedTab == ArchiveTab.LIBRARY,
            onClick = { onTabSelected(ArchiveTab.LIBRARY) },
            text = { Text(stringResource(R.string.grimoire_categories)) },
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
        val expandedDescription = stringResource(
            if (isCollapsed) R.string.grimoire_expand else R.string.grimoire_collapse,
        )
        Text(
            text = stringResource(R.string.grimoire_category_count, category.displayName(), count),
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (isCollapsed) "▸" else "▾",
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.semantics { contentDescription = expandedDescription },
        )
    }
}

/** Human-readable label for a [ContentCategory], for the Grimoire grouping headers. */
@Composable
internal fun ContentCategory.displayName(): String = stringResource(
    when (this) {
        ContentCategory.CANDLE_SPELLS -> R.string.category_candle_spells
        ContentCategory.HERBAL_RITUALS -> R.string.category_herbal_rituals
        ContentCategory.CORD_KNOT_WORK -> R.string.category_cord_knot
        ContentCategory.LUNAR_PRACTICES -> R.string.category_lunar
        ContentCategory.CHARMS_TALISMANS -> R.string.category_charms
        ContentCategory.CANDLE_MAGIC -> R.string.category_candle_magic
        ContentCategory.HERBS_BOTANICALS -> R.string.category_herbs
        ContentCategory.POTIONS_INFUSIONS -> R.string.category_potions
        ContentCategory.CRYSTALS_STONES -> R.string.category_crystals
        ContentCategory.CORD_KNOT_MAGIC -> R.string.category_cord_magic
        ContentCategory.MOON_MAGIC_TIMING -> R.string.category_moon_magic
        ContentCategory.ELEMENTAL_MAGIC -> R.string.category_elemental_magic
        ContentCategory.ALTARS_SACRED_SPACE -> R.string.category_altars
        ContentCategory.DIVINATION -> R.string.category_divination
        ContentCategory.ELEMENTAL_PRACTICES -> R.string.category_elemental_practices
        ContentCategory.PROTECTION_CLEANSING_WARDING -> R.string.category_protection_cleansing
        ContentCategory.PROTECTION -> R.string.category_protection
        ContentCategory.LOVE_AND_RELATIONSHIPS -> R.string.category_love
        ContentCategory.PROSPERITY_AND_ABUNDANCE -> R.string.category_prosperity
        ContentCategory.HEALING_AND_WELLNESS -> R.string.category_healing
        ContentCategory.DIVINATION_AND_CLARITY -> R.string.category_divination_clarity
        ContentCategory.BANISHING_AND_RELEASE -> R.string.category_banishing
        ContentCategory.HOME_AND_HEARTH -> R.string.category_home
        ContentCategory.MOON_AND_SEASONAL -> R.string.category_moon_seasonal
    },
)

@Composable
private fun EmptyArchive() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.grimoire_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
