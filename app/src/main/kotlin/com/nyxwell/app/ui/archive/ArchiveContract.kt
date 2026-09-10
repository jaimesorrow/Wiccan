package com.nyxwell.app.ui.archive

import com.nyxwell.app.core.error.AppError
import com.nyxwell.app.domain.model.Content
import com.nyxwell.app.domain.model.HistoryEntry

data class ArchiveUiState(
    val isLoading: Boolean = true,
    val entries: List<HistoryEntry> = emptyList(),
    val isDetailLoading: Boolean = false,
    val selectedContent: Content? = null,
)

sealed class ArchiveUiEvent {
    data class EntryClicked(val contentId: String) : ArchiveUiEvent()
    data object DismissDetail : ArchiveUiEvent()
}

sealed class ArchiveUiEffect {
    data class ShowSnackbar(val error: AppError) : ArchiveUiEffect()
}
