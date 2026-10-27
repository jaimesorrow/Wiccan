package com.jaimesorrow.wiccan.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val firstName: String = "",
    val lastName: String = "",
    val busy: Boolean = false,
    val notice: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun setFirstName(value: String) { _state.update { it.copy(firstName = value, notice = null) } }
    fun setLastName(value: String) { _state.update { it.copy(lastName = value, notice = null) } }
    fun resendVerification() = perform(
        { authRepository.sendEmailVerification() },
        "If verification is pending, a new email has been sent.",
    )
    fun checkVerification() = perform(
        { authRepository.refreshEmailVerification() },
        "Verification checked. Confirm your email before continuing.",
    )
    fun completeProfile() {
        val first = state.value.firstName.trim()
        val last = state.value.lastName.trim()
        if (first.isBlank() || last.isBlank() || first.length > 60 || last.length > 60) {
            _state.update { it.copy(notice = "Enter your first and last names (up to 60 characters each).") }
            return
        }
        perform({ authRepository.completeProfile(first, last) }, "Profile saved.")
    }
    fun signOut() = perform({ authRepository.signOut() }, "Signed out.")

    private fun perform(
        operation: suspend () -> AppResult<Unit>,
        successMessage: String,
    ) {
        if (state.value.busy) return
        _state.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            val result = operation()
            _state.update {
                it.copy(
                    busy = false,
                    notice = if (result is AppResult.Success) successMessage else
                        "Unable to complete this action. Check your connection and try again.",
                )
            }
        }
    }
}
