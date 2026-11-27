package com.jaimesorrow.wiccan.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import com.wiccan.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which onboarding gate is on screen; the root state machine decides, the screen reports it. */
enum class OnboardingStep { VERIFY_EMAIL, PROFILE }

data class OnboardingUiState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val busy: Boolean = false,
    /** The step currently shown. [notice] always belongs to this step. */
    val step: OnboardingStep = OnboardingStep.VERIFY_EMAIL,
    val notice: Int? = null,
) {
    /** The notice to show on [shownStep], or null when it was raised on a different step. */
    fun noticeFor(shownStep: OnboardingStep): Int? = notice.takeIf { step == shownStep }

    val usernameHasError: Boolean
        get() = username.isNotEmpty() && !USERNAME_PATTERN.matches(username.trim())
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /** Bumped on every account change; results of requests started for an earlier account are dropped. */
    private var accountGeneration = 0

    init {
        // Activity-scoped: never show one account's typed names or notices to the next.
        viewModelScope.launch {
            authRepository.accountChanges().collect {
                accountGeneration++
                _state.update { OnboardingUiState(step = it.step) }
            }
        }
    }

    /**
     * The same ViewModel serves both the verify-email and name steps. A notice
     * raised on one step (e.g. "Confirm your email before continuing") must not
     * carry over once the root moves the user on, so switching steps drops it.
     */
    fun onStepShown(step: OnboardingStep) {
        val deliveryFailed = step == OnboardingStep.VERIFY_EMAIL &&
            authRepository.consumeVerificationDeliveryFailure()
        _state.update {
            when {
                deliveryFailed -> it.copy(
                    step = step,
                    notice = R.string.error_verification_email_delivery,
                )
                it.step == step -> it
                else -> it.copy(step = step, notice = null)
            }
        }
    }

    fun setFirstName(value: String) {
        _state.update { it.copy(firstName = value, notice = null) }
    }
    fun setLastName(value: String) {
        _state.update { it.copy(lastName = value, notice = null) }
    }
    fun setUsername(value: String) {
        _state.update { it.copy(username = value.take(MAX_USERNAME_LENGTH), notice = null) }
    }
    fun resendVerification() = perform(
        { authRepository.sendEmailVerification() },
        R.string.onboarding_notice_resent,
    )
    fun checkVerification() = perform(
        { authRepository.refreshEmailVerification() },
        R.string.onboarding_notice_not_verified,
    )
    fun completeProfile() {
        val first = state.value.firstName.trim()
        val last = state.value.lastName.trim()
        val username = state.value.username.trim()
        if (first.isBlank() || last.isBlank() || first.length > 60 || last.length > 60 ||
            !USERNAME_PATTERN.matches(username)
        ) {
            _state.update { it.copy(notice = R.string.onboarding_notice_profile_invalid) }
            return
        }
        perform({ authRepository.completeProfile(first, last, username) }, R.string.onboarding_notice_saved)
    }
    fun signOut() = perform({ authRepository.signOut() }, R.string.onboarding_notice_signed_out)

    private fun perform(
        operation: suspend () -> AppResult<Unit>,
        @StringRes successMessage: Int,
    ) {
        if (state.value.busy) return
        val startedOn = state.value.step
        val generation = accountGeneration
        _state.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            val result = operation()
            // Started for a previous account: its busy flag and notice were already reset.
            if (generation != accountGeneration) return@launch
            val message = when (result) {
                is AppResult.Success -> successMessage
                is AppResult.Failure -> if (result.error is com.jaimesorrow.wiccan.core.error.AppError.UsernameTaken) {
                    R.string.error_username_taken
                } else {
                    R.string.onboarding_notice_failed
                }
            }
            _state.update {
                // If the user was moved to another step meanwhile, the result
                // no longer describes what they're looking at.
                it.copy(busy = false, notice = if (it.step == startedOn) message else null)
            }
        }
    }
}

private const val MAX_USERNAME_LENGTH = 24
private val USERNAME_PATTERN = Regex("^[A-Za-z0-9_]{3,$MAX_USERNAME_LENGTH}\$")
