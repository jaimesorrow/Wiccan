package com.jaimesorrow.wiccan.ui.archive

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.ads.GrimoireBannerAd
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import com.jaimesorrow.wiccan.seasonal.grimoireMoonTrack
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanEmblem
import com.jaimesorrow.wiccan.ui.brand.WiccanLineArt
import com.jaimesorrow.wiccan.ui.brand.WiccanLineArtKind
import com.jaimesorrow.wiccan.ui.brand.lineArt
import com.jaimesorrow.wiccan.ui.brand.wiccanEmblemColor
import com.jaimesorrow.wiccan.ui.brand.wiccanPageWash
import com.jaimesorrow.wiccan.ui.theme.GrimoireBannerSlot
import com.jaimesorrow.wiccan.ui.theme.GrimoireInkLabel
import com.jaimesorrow.wiccan.ui.theme.GrimoireMoonPhases
import com.jaimesorrow.wiccan.ui.theme.GrimoireOrnamentalDivider
import com.jaimesorrow.wiccan.ui.theme.GrimoireSpellMapWash
import com.jaimesorrow.wiccan.ui.theme.MidnightGrimoireTheme
import com.jaimesorrow.wiccan.ui.theme.WiccanDisplayFont
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.jaimesorrow.wiccan.ui.theme.grimoireCornerFlourishes
import com.jaimesorrow.wiccan.ui.theme.grimoireFieldColors
import com.wiccan.app.R
import java.time.LocalDate

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
    // Non-subscribers only; renders nothing for Premium (see GrimoireBannerAd).
    bottomAd: @Composable () -> Unit = { GrimoireBannerAd() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ArchiveUiEffect.NavigateToDetail -> onNavigateToDetail(effect.contentId)
            }
        }
    }

    ArchiveScreenContent(state = state, onEvent = viewModel::onEvent, modifier = modifier, bottomBar = bottomAd)
}

@Composable
internal fun ArchiveScreenContent(
    state: ArchiveUiState,
    onEvent: (ArchiveUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    MidnightGrimoireTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = bottomBar,
        ) { insets ->
            Box(modifier = Modifier.padding(insets).fillMaxSize().wiccanPageWash()) {
                GrimoireSpellMapWash(Modifier.matchParentSize())
                Column(modifier = Modifier.fillMaxSize()) {
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
                    WiccanBotanicalAccent(modifier = Modifier.align(Alignment.CenterHorizontally), width = 116.dp)
                    Text(
                        text = stringResource(R.string.grimoire_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = WiccanDisplayFont),
                        color = WiccanPaletteColors.Grimoire.Gold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                    GrimoireOrnamentalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
                    Text(
                        text = stringResource(R.string.grimoire_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    val archiveMoon = remember { grimoireMoonTrack(LocalDate.now()) }
                    GrimoireMoonPhases(
                        active = archiveMoon.trackIndex,
                        activePhaseName = archiveMoon.name,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                    GrimoireInkLabel(
                        text = stringResource(R.string.motif_kept_pages),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            WiccanLineArtKind.CrystalCitrine,
                            WiccanLineArtKind.CrystalClear,
                            WiccanLineArtKind.CrystalSmoky,
                            WiccanLineArtKind.CrystalObsidian,
                        ).forEach { art ->
                            WiccanLineArt(
                                kind = art,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    val loadingDescription = stringResource(R.string.grimoire_loading)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            state.isLoading -> Box(
                                Modifier.fillMaxSize().semantics { contentDescription = loadingDescription },
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                            state.entries.isEmpty() && state.error != null -> {
                                val historyError = checkNotNull(state.error)
                                Box(
                                    Modifier.fillMaxSize().padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = historyError.toUserMessage(LocalResources.current),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            else -> {
                                val resources = LocalResources.current
                                val visibleEntries = state.visibleEntries(
                                    localizedCategory = { category ->
                                        resources.getString(category.labelResId())
                                    },
                                    localizedType = { type -> resources.getString(type.labelResId()) },
                                )
                                Box(Modifier.fillMaxSize()) {
                                    Column(Modifier.fillMaxSize()) {
                                        GrimoireSearch(
                                            query = state.searchQuery,
                                            onQueryChanged = { onEvent(ArchiveUiEvent.SearchQueryChanged(it)) },
                                            onClear = { onEvent(ArchiveUiEvent.SearchCleared) },
                                        )
                                        ArchiveTabRow(
                                            selectedTab = state.selectedTab,
                                            onTabSelected = { onEvent(ArchiveUiEvent.TabSelected(it)) },
                                        )
                                        if (state.entries.isNotEmpty() && visibleEntries.isEmpty()) {
                                            EmptySearchResults()
                                        } else {
                                            when (state.selectedTab) {
                                                ArchiveTab.TIMELINE -> if (state.entries.isEmpty()) {
                                                    EmptyArchive()
                                                } else {
                                                    TimelineList(
                                                        entries = visibleEntries,
                                                        onEntryClicked = { onEvent(ArchiveUiEvent.EntryClicked(it)) },
                                                    )
                                                }
                                                ArchiveTab.LIBRARY -> LibraryList(
                                                    entries = visibleEntries,
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
                    GrimoireBannerSlot()
                }
            }
        }
    }
}

@Composable
private fun GrimoireSearch(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        label = { Text(stringResource(R.string.grimoire_search_label)) },
        placeholder = { Text(stringResource(R.string.grimoire_search_hint)) },
        colors = grimoireFieldColors(),
        trailingIcon = if (query.isBlank()) {
            null
        } else {
            {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.grimoire_search_clear),
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("grimoire-search"),
    )
}

@Composable
private fun ArchiveTabRow(selectedTab: ArchiveTab, onTabSelected: (ArchiveTab) -> Unit) {
    val gold = WiccanPaletteColors.Grimoire.Gold
    val muted = WiccanPaletteColors.Grimoire.Muted
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = WiccanPaletteColors.Grimoire.Background,
        contentColor = gold,
        divider = {
            HorizontalDivider(
                color = WiccanPaletteColors.Grimoire.Border.copy(alpha = 0.5f),
                thickness = 1.dp,
            )
        },
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                color = gold,
                height = 2.dp,
            )
        },
    ) {
        Tab(
            selected = selectedTab == ArchiveTab.TIMELINE,
            onClick = { onTabSelected(ArchiveTab.TIMELINE) },
            selectedContentColor = gold,
            unselectedContentColor = muted,
            text = { Text(stringResource(R.string.grimoire_timeline)) },
        )
        Tab(
            selected = selectedTab == ArchiveTab.LIBRARY,
            onClick = { onTabSelected(ArchiveTab.LIBRARY) },
            selectedContentColor = gold,
            unselectedContentColor = muted,
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
    val entriesByCategory = entries.groupBy { it.category }
    val populatedCurrentCategories = ContentCategory.libraryCategories.filter(entriesByCategory::containsKey)
    val populatedLegacyCategories =
        entriesByCategory.keys.filterNot(ContentCategory.libraryCategories::contains).sortedBy { it.ordinal }
    val emptyCurrentCategories = ContentCategory.libraryCategories.filterNot(entriesByCategory::containsKey)
    val categories = populatedCurrentCategories + populatedLegacyCategories + emptyCurrentCategories

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        categories.forEach { category ->
            val categoryEntries = entriesByCategory[category].orEmpty()
            val isCollapsed = category in collapsedCategories
            item(key = "header-${category.name}") {
                CategoryHeader(
                    category = category,
                    count = categoryEntries.size,
                    isCollapsed = isCollapsed,
                    onClick = if (categoryEntries.isEmpty()) {
                        null
                    } else {
                        { onCategoryToggled(category) }
                    },
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
private fun CategoryHeader(
    category: ContentCategory,
    count: Int,
    isCollapsed: Boolean,
    onClick: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(6.dp)
    val artwork = category.lineArt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(WiccanPaletteColors.Grimoire.CardFill, shape)
            .border(1.dp, WiccanPaletteColors.Grimoire.Border, shape)
            .grimoireCornerFlourishes(arm = 10.dp)
            .then(
                if (onClick == null) {
                    Modifier.semantics { disabled() }
                } else {
                    Modifier.clickable(
                        onClickLabel = if (isCollapsed) "Show this category" else "Hide this category",
                        onClick = onClick,
                    )
                },
            )
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (artwork != null) {
            WiccanLineArt(
                kind = artwork,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(width = 56.dp, height = 42.dp),
            )
        }
        Text(
            text = stringResource(R.string.grimoire_category_count, category.displayName(), count),
            style = MaterialTheme.typography.titleSmall,
            color = WiccanPaletteColors.Grimoire.Text,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            Text(
                text = if (isCollapsed) "▸" else "▾",
                style = MaterialTheme.typography.titleSmall,
                color = WiccanPaletteColors.Grimoire.Gold,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
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

@Composable
private fun EmptySearchResults() {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.grimoire_search_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
