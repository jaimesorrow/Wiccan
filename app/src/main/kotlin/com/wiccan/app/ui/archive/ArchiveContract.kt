package com.wiccan.app.ui.archive

import com.wiccan.app.domain.model.ContentCategory
import com.wiccan.app.domain.model.HistoryEntry

/** Which view of the Book of Shadows is showing: the chronological feed or the category Library. */
enum class ArchiveTab {
    TIMELINE,
    LIBRARY,
}

data class ArchiveUiState(
    val isLoading: Boolean = true,
    val entries: List<HistoryEntry> = emptyList(),
    val selectedTab: ArchiveTab = ArchiveTab.TIMELINE,
    /** Categories the user has collapsed in the Library view. Expanded is the default. */
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
