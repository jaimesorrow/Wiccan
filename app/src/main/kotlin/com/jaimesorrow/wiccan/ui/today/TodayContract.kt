package com.jaimesorrow.wiccan.ui.today

import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.domain.model.Content

/**
 * State holds only display data — no error field. Refresh failures surface
 * exclusively via [TodayUiEffect.ShowSnackbar], per the project's MVI
 * convention.
 */
data class TodayUiState(
    val isLoading: Boolean = true,
    val content: Content? = null,
    val newLessonPending: Boolean = false,
)

sealed class TodayUiEvent {
    data object Retry : TodayUiEvent()
}

sealed class TodayUiEffect {
    data class ShowSnackbar(val error: AppError) : TodayUiEffect()
}
