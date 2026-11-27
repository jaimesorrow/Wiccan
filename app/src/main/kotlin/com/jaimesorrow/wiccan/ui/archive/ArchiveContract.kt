package com.jaimesorrow.wiccan.ui.archive

import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import java.util.Locale

/** Which view of the Grimoire is showing: the chronological feed or the categories in Grimoire. */
enum class ArchiveTab {
    TIMELINE,
    LIBRARY,
}

data class ArchiveUiState(
    val isLoading: Boolean = true,
    val entries: List<HistoryEntry> = emptyList(),
    val error: AppError? = null,
    val selectedTab: ArchiveTab = ArchiveTab.TIMELINE,
    val searchQuery: String = "",
    val isEtymologyExpanded: Boolean = false,
    /** Categories the user has collapsed in the category view. Expanded is the default. */
    val collapsedCategories: Set<ContentCategory> = emptySet(),
)

internal val ArchiveUiState.visibleEntries: List<HistoryEntry>
    get() {
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        if (query.isEmpty()) return entries
        return entries.filter { entry ->
            listOf(
                entry.title,
                entry.tradition,
                entry.type.name.replace('_', ' '),
                entry.category.name.replace('_', ' '),
            ).any { it.lowercase(Locale.ROOT).contains(query) }
        }
    }

sealed class ArchiveUiEvent {
    data class EntryClicked(val contentId: String) : ArchiveUiEvent()
    data class TabSelected(val tab: ArchiveTab) : ArchiveUiEvent()
    data class CategoryToggled(val category: ContentCategory) : ArchiveUiEvent()
    data class SearchQueryChanged(val query: String) : ArchiveUiEvent()
    data object SearchCleared : ArchiveUiEvent()
    data object EtymologyToggled : ArchiveUiEvent()
}

sealed class ArchiveUiEffect {
    data class NavigateToDetail(val contentId: String) : ArchiveUiEffect()
}
