package com.jaimesorrow.wiccan.ui.policy

import com.jaimesorrow.wiccan.core.error.AppError

/** Each agreement the onboarding gate requires; every one must be checked to continue. */
enum class PolicyAgreement {
    ADULT,
    TERMS,
    PRIVACY,
    NO_GUARANTEE,
}

data class PolicyAcceptanceUiState(
    val agreed: Set<PolicyAgreement> = emptySet(),
    /** Optional, unchecked by default, and never required to continue. */
    val shareAnalytics: Boolean = false,
    val isSubmitting: Boolean = false,
) {
    val allAgreed: Boolean get() = agreed.containsAll(PolicyAgreement.entries)
    val canContinue: Boolean get() = allAgreed && !isSubmitting
}

sealed class PolicyAcceptanceUiEvent {
    data class AgreementChanged(val agreement: PolicyAgreement, val checked: Boolean) : PolicyAcceptanceUiEvent()
    data class ShareAnalyticsChanged(val share: Boolean) : PolicyAcceptanceUiEvent()
    data object ContinueClicked : PolicyAcceptanceUiEvent()
}

sealed class PolicyAcceptanceUiEffect {
    data class ShowSnackbar(val error: AppError) : PolicyAcceptanceUiEffect()
}
