package com.jaimesorrow.wiccan.ui.auth

import com.jaimesorrow.wiccan.core.error.AppError

enum class SignInMode { SIGN_IN, CREATE_ACCOUNT, FORGOT_PASSWORD }

data class SignInUiState(
    val isLoading: Boolean = false,
    val mode: SignInMode = SignInMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val passwordResetSent: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isGoogleSignInAvailable: Boolean = false,
)

sealed class SignInUiEvent {
    data class EmailChanged(val value: String) : SignInUiEvent()
    data class PasswordChanged(val value: String) : SignInUiEvent()
    data class ConfirmPasswordChanged(val value: String) : SignInUiEvent()
    data object TogglePasswordVisibility : SignInUiEvent()
    data object ToggleMode : SignInUiEvent()
    data object BackToSignIn : SignInUiEvent()
    data object SubmitEmailAuth : SignInUiEvent()
    data object ForgotPasswordClicked : SignInUiEvent()
    data object SubmitPasswordReset : SignInUiEvent()
}

sealed class SignInUiEffect {
    data class ShowSnackbar(val error: AppError) : SignInUiEffect()
    data object PasswordResetEmailSent : SignInUiEffect()
}
