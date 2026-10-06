package com.jaimesorrow.wiccan.ui.archive

import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.HistoryEntry

/** Which view of the Grimoire is showing: the chronological feed or the categories in Grimoire. */
enum class ArchiveTab {
    TIMELINE,
    LIBRARY,
}

data class ArchiveUiState(
    val isLoading: Boolean = true,
    val entries: List<HistoryEntry> = emptyList(),
    val selectedTab: ArchiveTab = ArchiveTab.TIMELINE,
    /** Categories the user has collapsed in the category view. Expanded is the default. */
    val collapsedCategories: Set<ContentCategory> = emptySet(),
)

sealed class ArchiveUiEvent {
    data class EntryClicked(val contentId: String) : ArchiveUiEvent()
    data class TabSelected(val tab: ArchiveTab) : ArchiveUiEvent()
    data class CategoryToggled(val category: ContentCategory) : ArchiveUiEvent()
}

sealed class ArchiveUiEffect {
    data class NavigateToDetail(val contentId: String) : ArchiveUiEffect()
}
