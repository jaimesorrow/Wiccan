package com.jaimesorrow.wiccan.ui.policy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.analytics.AnalyticsConsentManager
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val analyticsConsentManager: AnalyticsConsentManager,
) : ViewModel() {

    private val _state = MutableStateFlow(PolicyAcceptanceUiState())
    val state: StateFlow<PolicyAcceptanceUiState> = _state.asStateFlow()

    private val effectChannel = Channel<PolicyAcceptanceUiEffect>(Channel.BUFFERED)
    val effects: Flow<PolicyAcceptanceUiEffect> = effectChannel.receiveAsFlow()

    /** Set once the user touches the analytics box, so the late stored value never overrides them. */
    private var analyticsChoiceEdited = false

    init {
        // A returning user re-accepting updated policies keeps their earlier analytics choice.
        viewModelScope.launch {
            val stored = runCatching { analyticsConsentManager.consent.first() }.getOrDefault(false)
            if (!analyticsChoiceEdited) _state.update { it.copy(shareAnalytics = stored) }
        }
    }

    fun onEvent(event: PolicyAcceptanceUiEvent) {
        when (event) {
            is PolicyAcceptanceUiEvent.AgreementChanged -> _state.update {
                it.copy(agreed = if (event.checked) it.agreed + event.agreement else it.agreed - event.agreement)
            }
            is PolicyAcceptanceUiEvent.ShareAnalyticsChanged -> {
                analyticsChoiceEdited = true
                _state.update { it.copy(shareAnalytics = event.share) }
            }
            PolicyAcceptanceUiEvent.ContinueClicked -> acceptTerms()
        }
    }

    private fun acceptTerms() {
        // The button is disabled until every agreement is checked; this guards the event itself too.
        if (!_state.value.canContinue) return
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            // Analytics is optional: a failed save never blocks accepting the policies.
            if (!analyticsConsentManager.setConsent(_state.value.shareAnalytics)) {
                effectChannel.send(PolicyAcceptanceUiEffect.ShowSnackbar(CONSENT_NOT_SAVED))
            }
            when (val result = authRepository.acceptTerms()) {
                is AppResult.Success -> _state.update { it.copy(isSubmitting = false) }
                is AppResult.Failure -> {
                    _state.update { it.copy(isSubmitting = false) }
                    effectChannel.send(PolicyAcceptanceUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }

    private companion object {
        val CONSENT_NOT_SAVED = AppError.Unknown("Your analytics choice couldn't be saved. You can set it again in Settings.")
    }
}
