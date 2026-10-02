package com.wiccan.app.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.repository.AuthRepository
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
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SignInUiState(isGoogleSignInAvailable = authRepository.isGoogleSignInAvailable()),
    )
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val effectChannel = Channel<SignInUiEffect>(Channel.BUFFERED)
    val effects: Flow<SignInUiEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: SignInUiEvent) {
        when (event) {
            is SignInUiEvent.EmailChanged -> _state.update { it.copy(email = event.value) }
            is SignInUiEvent.PasswordChanged -> _state.update { it.copy(password = event.value) }
            SignInUiEvent.ToggleMode -> _state.update {
                it.copy(mode = if (it.mode == SignInMode.SIGN_IN) SignInMode.CREATE_ACCOUNT else SignInMode.SIGN_IN)
            }
            SignInUiEvent.SubmitEmailAuth -> submitEmailAuth()
            SignInUiEvent.ForgotPasswordClicked -> sendPasswordReset()
        }
    }

    fun onGoogleSignInClick(context: Context) {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            handleAuthResult(authRepository.signInWithGoogle(context))
        }
    }

    private fun submitEmailAuth() {
        val email = _state.value.email
        val password = _state.value.password
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = when (_state.value.mode) {
                SignInMode.SIGN_IN -> authRepository.signInWithEmail(email, password)
                SignInMode.CREATE_ACCOUNT -> authRepository.createAccountWithEmail(email, password)
            }
            handleAuthResult(result)
        }
    }

    private fun sendPasswordReset() {
        val email = _state.value.email
        viewModelScope.launch {
            when (val result = authRepository.sendPasswordReset(email)) {
                is AppResult.Success -> effectChannel.send(SignInUiEffect.PasswordResetEmailSent)
                is AppResult.Failure -> effectChannel.send(SignInUiEffect.ShowSnackbar(result.error))
            }
        }
    }

    private suspend fun handleAuthResult(result: AppResult<Unit>) {
        _state.update { it.copy(isLoading = false) }
        if (result is AppResult.Failure) {
            effectChannel.send(SignInUiEffect.ShowSnackbar(result.error))
        }
        // On success, observeAuthState() (collected at the app root) transitions
        // the UI away from this screen — no local state change needed here.
    }
}
