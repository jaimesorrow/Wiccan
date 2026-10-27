package com.jaimesorrow.wiccan.ui.settings

import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode

data class SettingsUiState(
    val isDeleting: Boolean = false,
    val isAccountBusy: Boolean = false,
    val accountEmail: String = "",
    val newEmail: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val showDeleteConfirmation: Boolean = false,
    val tier: ContentTier = ContentTier.FREE,
    val notificationsEnabled: Boolean = true,
    val themeMode: WiccanThemeMode = WiccanThemeMode.DEFAULT,
)

sealed class SettingsUiEvent {
    data object SignOutClicked : SettingsUiEvent()
    data class NewEmailChanged(val value: String) : SettingsUiEvent()
    data class FirstNameChanged(val value: String) : SettingsUiEvent()
    data class LastNameChanged(val value: String) : SettingsUiEvent()
    data object SaveNameClicked : SettingsUiEvent()
    data object ChangeEmailClicked : SettingsUiEvent()
    data object ResetPasswordClicked : SettingsUiEvent()
    data object DeleteAccountClicked : SettingsUiEvent()
    data object ConfirmDeleteAccount : SettingsUiEvent()
    data object CancelDeleteAccount : SettingsUiEvent()
    data class NotificationsToggled(val enabled: Boolean) : SettingsUiEvent()
    data class ThemeModeSelected(val mode: WiccanThemeMode) : SettingsUiEvent()
}

sealed class SettingsUiEffect {
    data class ShowSnackbar(val error: AppError) : SettingsUiEffect()
    data class ShowMessage(val message: String) : SettingsUiEffect()
}
