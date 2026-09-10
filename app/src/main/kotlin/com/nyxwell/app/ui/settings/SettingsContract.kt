package com.nyxwell.app.ui.settings

import com.nyxwell.app.core.error.AppError

data class SettingsUiState(
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
)

sealed class SettingsUiEvent {
    data object SignOutClicked : SettingsUiEvent()
    data object DeleteAccountClicked : SettingsUiEvent()
    data object ConfirmDeleteAccount : SettingsUiEvent()
    data object CancelDeleteAccount : SettingsUiEvent()
}

sealed class SettingsUiEffect {
    data class ShowSnackbar(val error: AppError) : SettingsUiEffect()
}
