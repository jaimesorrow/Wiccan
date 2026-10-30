package com.jaimesorrow.wiccan.ui.auth

import android.content.Context
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

private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@HiltViewModel
class SignInViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(
        SignInUiState(isGoogleSignInAvailable = authRepository.isGoogleSignInAvailable()),
    )
    val state: StateFlow<SignInUiState> = _state.asStateFlow()
    private val effectChannel = Channel<SignInUiEffect>(Channel.BUFFERED)
    val effects: Flow<SignInUiEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: SignInUiEvent) {
        when (event) {
            is SignInUiEvent.EmailChanged -> _state.update {
                it.copy(email = event.value, emailError = null, passwordResetSent = false)
            }
            is SignInUiEvent.PasswordChanged -> _state.update {
                it.copy(password = event.value, passwordError = null)
            }
            is SignInUiEvent.ConfirmPasswordChanged -> _state.update {
                it.copy(confirmPassword = event.value, confirmPasswordError = null)
            }
            SignInUiEvent.TogglePasswordVisibility -> _state.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            SignInUiEvent.ToggleMode -> _state.update {
                it.copy(
                    mode = if (it.mode == SignInMode.SIGN_IN) SignInMode.CREATE_ACCOUNT else SignInMode.SIGN_IN,
                    password = "", confirmPassword = "", isPasswordVisible = false,
                    emailError = null, passwordError = null, confirmPasswordError = null,
                )
            }
            SignInUiEvent.BackToSignIn -> _state.update {
                it.copy(mode = SignInMode.SIGN_IN, password = "", passwordResetSent = false, emailError = null)
            }
            SignInUiEvent.ForgotPasswordClicked -> _state.update {
                it.copy(mode = SignInMode.FORGOT_PASSWORD, password = "", emailError = null, passwordResetSent = false)
            }
            SignInUiEvent.SubmitEmailAuth -> submitEmailAuth()
            SignInUiEvent.SubmitPasswordReset -> sendPasswordReset()
        }
    }

    fun onGoogleSignInClick(context: Context) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch { handleAuthResult(authRepository.signInWithGoogle(context)) }
    }

    private fun submitEmailAuth() {
        val current = _state.value
        if (current.isLoading || current.mode == SignInMode.FORGOT_PASSWORD) return
        val email = current.email.trim()
        val password = current.password
        val creating = current.mode == SignInMode.CREATE_ACCOUNT
        val emailError = when {
            email.isBlank() -> "Enter your email address."
            !EMAIL_PATTERN.matches(email) -> "Enter a valid email address."
            else -> null
        }
        val passwordError = when {
            password.isBlank() -> "Enter your password."
            password.length < 6 -> "Password must be at least 6 characters."
            else -> null
        }
        val confirmPasswordError = if (creating && current.confirmPassword != password) "Passwords do not match." else null
        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _state.update {
                it.copy(
                    email = email,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }
        _state.update { it.copy(email = email, isLoading = true) }
        viewModelScope.launch {
            val result = if (creating) {
                authRepository.createAccountWithEmail(email, password)
            } else {
                authRepository.signInWithEmail(email, password)
            }
            handleAuthResult(result)
        }
    }

    private fun sendPasswordReset() {
        val email = _state.value.email.trim()
        if (_state.value.isLoading) return
        if (email.isBlank() || !EMAIL_PATTERN.matches(email)) {
            _state.update { it.copy(email = email, emailError = "Enter a valid email address.") }
            return
        }
        _state.update { it.copy(email = email, isLoading = true) }
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            _state.update { it.copy(isLoading = false) }
            when (result) {
                is AppResult.Success -> {
                    _state.update { it.copy(passwordResetSent = true) }
                    effectChannel.send(SignInUiEffect.PasswordResetEmailSent)
                }
                is AppResult.Failure -> effectChannel.send(SignInUiEffect.ShowSnackbar(result.error))
            }
        }
    }

    private suspend fun handleAuthResult(result: AppResult<Unit>) {
        _state.update { it.copy(isLoading = false) }
        if (result is AppResult.Failure) effectChannel.send(SignInUiEffect.ShowSnackbar(result.error))
        // Auth success never unlocks content; the root gating state machine decides.
    }
}
