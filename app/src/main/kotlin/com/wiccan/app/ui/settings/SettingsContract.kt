package com.wiccan.app.ui.settings

import com.wiccan.app.core.error.AppError
import com.wiccan.app.domain.model.WiccanThemeMode
import com.wiccan.app.domain.model.ContentTier

data class SettingsUiState(
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val tier: ContentTier = ContentTier.FREE,
    val notificationsEnabled: Boolean = true,
    val themeMode: WiccanThemeMode = WiccanThemeMode.DEFAULT,
)

sealed class SettingsUiEvent {
    data object SignOutClicked : SettingsUiEvent()
    data object DeleteAccountClicked : SettingsUiEvent()
    data object ConfirmDeleteAccount : SettingsUiEvent()
    data object CancelDeleteAccount : SettingsUiEvent()
    data class NotificationsToggled(val enabled: Boolean) : SettingsUiEvent()
    data class ThemeModeSelected(val mode: WiccanThemeMode) : SettingsUiEvent()
}

sealed class SettingsUiEffect {
    data class ShowSnackbar(val error: AppError) : SettingsUiEffect()
}
