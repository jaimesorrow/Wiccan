package com.wiccan.app.ui.settings

import com.wiccan.app.core.error.AppError
import com.wiccan.app.domain.model.ContentTier

data class SettingsUiState(
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val tier: ContentTier = ContentTier.FREE,
    val notificationsEnabled: Boolean = true,
)

sealed class SettingsUiEvent {
    data object SignOutClicked : SettingsUiEvent()
    data object DeleteAccountClicked : SettingsUiEvent()
    data object ConfirmDeleteAccount : SettingsUiEvent()
    data object CancelDeleteAccount : SettingsUiEvent()
    data class NotificationsToggled(val enabled: Boolean) : SettingsUiEvent()
}

sealed class SettingsUiEffect {
    data class ShowSnackbar(val error: AppError) : SettingsUiEffect()
}
