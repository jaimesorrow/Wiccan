package com.jaimesorrow.wiccan.ui.policy

import com.jaimesorrow.wiccan.core.error.AppError

data class PolicyAcceptanceUiState(
    val isChecked: Boolean = false,
    val isSubmitting: Boolean = false,
)

sealed class PolicyAcceptanceUiEvent {
    data class CheckedChanged(val checked: Boolean) : PolicyAcceptanceUiEvent()
    data object ContinueClicked : PolicyAcceptanceUiEvent()
}

sealed class PolicyAcceptanceUiEffect {
    data class ShowSnackbar(val error: AppError) : PolicyAcceptanceUiEffect()
}
