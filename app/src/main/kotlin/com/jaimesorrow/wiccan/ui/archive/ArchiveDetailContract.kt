package com.jaimesorrow.wiccan.ui.archive

import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.domain.model.Content

data class ArchiveDetailUiState(
    val isLoading: Boolean = true,
    val content: Content? = null,
)

sealed class ArchiveDetailUiEffect {
    data class ShowSnackbar(val error: AppError) : ArchiveDetailUiEffect()
}
