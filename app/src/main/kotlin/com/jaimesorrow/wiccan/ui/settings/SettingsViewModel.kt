package com.jaimesorrow.wiccan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.local.ThemePreferences
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import com.jaimesorrow.wiccan.domain.usecase.DeleteAccountUseCase
import com.jaimesorrow.wiccan.widget.WiccanWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val subscriptionRepository: SubscriptionRepository,
    private val notificationRepository: NotificationRepository,
    private val themePreferences: ThemePreferences,
    private val wiccanWidgetUpdater: WiccanWidgetUpdater,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val effectChannel = Channel<SettingsUiEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsUiEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            subscriptionRepository.observeTier().collect { tier ->
                _state.update { it.copy(tier = tier) }
            }
        }
        viewModelScope.launch {
            notificationRepository.notificationsEnabled.collect { enabled ->
                _state.update { it.copy(notificationsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.SignOutClicked -> signOut()
            SettingsUiEvent.DeleteAccountClicked -> _state.update { it.copy(showDeleteConfirmation = true) }
            SettingsUiEvent.CancelDeleteAccount -> _state.update { it.copy(showDeleteConfirmation = false) }
            SettingsUiEvent.ConfirmDeleteAccount -> deleteAccount()
            is SettingsUiEvent.NotificationsToggled -> toggleNotifications(event.enabled)
            is SettingsUiEvent.ThemeModeSelected -> selectThemeMode(event.mode)
        }
    }

    private fun selectThemeMode(mode: WiccanThemeMode) {
        // Purely local persistence (no server/AppResult involved) - the
        // DataStore flow collected in init() reflects the new value back
        // into state as soon as the write completes.
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
            wiccanWidgetUpdater.updateAll()
        }
    }

    private fun toggleNotifications(enabled: Boolean) {
        // Optimistic: the switch should feel instant. Reverted if the
        // server call fails, so the UI never claims a state that didn't
        // actually take effect.
        _state.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch {
            when (val result = notificationRepository.setNotificationsEnabled(enabled)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    _state.update { it.copy(notificationsEnabled = !enabled) }
                    effectChannel.send(SettingsUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            // Best-effort: a signed-out device shouldn't keep receiving pushes
            // meant for whoever signs in next. A failure here (offline, etc.)
            // must never block the actual sign-out.
            notificationRepository.unregisterToken()
            authRepository.signOut()
        }
    }

    private fun deleteAccount() {
        _state.update { it.copy(isDeleting = true, showDeleteConfirmation = false) }
        viewModelScope.launch {
            when (val result = deleteAccountUseCase()) {
                is AppResult.Success -> _state.update { it.copy(isDeleting = false) }
                is AppResult.Failure -> {
                    _state.update { it.copy(isDeleting = false) }
                    effectChannel.send(SettingsUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }
}
