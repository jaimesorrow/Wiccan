package com.jaimesorrow.wiccan.ui.auth

import com.jaimesorrow.wiccan.core.error.AppError

enum class SignInMode {
    SIGN_IN,
    CREATE_ACCOUNT,
}

data class SignInUiState(
    val isLoading: Boolean = false,
    val mode: SignInMode = SignInMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val isGoogleSignInAvailable: Boolean = false,
)

sealed class SignInUiEvent {
    data class EmailChanged(val value: String) : SignInUiEvent()
    data class PasswordChanged(val value: String) : SignInUiEvent()
    data object ToggleMode : SignInUiEvent()
    data object SubmitEmailAuth : SignInUiEvent()
    data object ForgotPasswordClicked : SignInUiEvent()
}

sealed class SignInUiEffect {
    data class ShowSnackbar(val error: AppError) : SignInUiEffect()
    data object PasswordResetEmailSent : SignInUiEffect()
}
