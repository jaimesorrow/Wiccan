package com.jaimesorrow.wiccan.ui.policy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
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

/**
 * Drives the required-agreement gate shown whenever [RootState][com.jaimesorrow.wiccan.domain.model.RootState]
 * is `RequiresPolicyAcceptance`. On success there is nothing further to
 * do here - [com.jaimesorrow.wiccan.ui.auth.RootViewModel]'s own Firestore
 * listener observes the write and moves root state to `SignedIn` on its
 * own, the same way [com.jaimesorrow.wiccan.domain.model.AccountStatus.Initializing]
 * resolves once the profile-creation trigger lands.
 */
@HiltViewModel
class PolicyAcceptanceViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PolicyAcceptanceUiState())
    val state: StateFlow<PolicyAcceptanceUiState> = _state.asStateFlow()

    private val effectChannel = Channel<PolicyAcceptanceUiEffect>(Channel.BUFFERED)
    val effects: Flow<PolicyAcceptanceUiEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: PolicyAcceptanceUiEvent) {
        when (event) {
            is PolicyAcceptanceUiEvent.CheckedChanged -> _state.update { it.copy(isChecked = event.checked) }
            PolicyAcceptanceUiEvent.ContinueClicked -> acceptTerms()
        }
    }

    private fun acceptTerms() {
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            when (val result = authRepository.acceptTerms()) {
                is AppResult.Success -> _state.update { it.copy(isSubmitting = false) }
                is AppResult.Failure -> {
                    _state.update { it.copy(isSubmitting = false) }
                    effectChannel.send(PolicyAcceptanceUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }
}
