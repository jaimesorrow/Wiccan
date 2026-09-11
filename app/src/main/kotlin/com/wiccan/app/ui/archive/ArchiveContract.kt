package com.wiccan.app.ui.archive

import com.wiccan.app.domain.model.HistoryEntry

data class ArchiveUiState(
    val isLoading: Boolean = true,
    val entries: List<HistoryEntry> = emptyList(),
)

sealed class ArchiveUiEvent {
    data class EntryClicked(val contentId: String) : ArchiveUiEvent()
}

sealed class ArchiveUiEffect {
    data class NavigateToDetail(val contentId: String) : ArchiveUiEffect()
}
