package com.wiccan.app.ui.archive

import com.wiccan.app.core.error.AppError
import com.wiccan.app.domain.model.Content

data class ArchiveDetailUiState(
    val isLoading: Boolean = true,
    val content: Content? = null,
)

sealed class ArchiveDetailUiEffect {
    data class ShowSnackbar(val error: AppError) : ArchiveDetailUiEffect()
}
