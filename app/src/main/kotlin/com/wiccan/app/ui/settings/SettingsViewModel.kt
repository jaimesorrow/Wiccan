package com.wiccan.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.repository.AuthRepository
import com.wiccan.app.domain.repository.SubscriptionRepository
import com.wiccan.app.domain.usecase.DeleteAccountUseCase
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
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.SignOutClicked -> signOut()
            SettingsUiEvent.DeleteAccountClicked -> _state.update { it.copy(showDeleteConfirmation = true) }
            SettingsUiEvent.CancelDeleteAccount -> _state.update { it.copy(showDeleteConfirmation = false) }
            SettingsUiEvent.ConfirmDeleteAccount -> deleteAccount()
        }
    }

    private fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
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
