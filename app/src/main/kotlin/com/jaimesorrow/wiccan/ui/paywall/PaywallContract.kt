package com.jaimesorrow.wiccan.ui.paywall

import com.jaimesorrow.wiccan.core.error.AppError

data class PaywallUiState(
    val isLoadingPrice: Boolean = true,
    val price: String? = null,
    val hasFreeTrial: Boolean = false,
    val isRestoring: Boolean = false,
)

sealed class PaywallUiEvent {
    data object RestorePurchasesClicked : PaywallUiEvent()
    data object RetryPriceClicked : PaywallUiEvent()
}

sealed class PaywallUiEffect {
    data class ShowSnackbar(val error: AppError) : PaywallUiEffect()
    data object PurchasesRestored : PaywallUiEffect()
}
