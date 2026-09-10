package com.nyxwell.app.ui.paywall

import com.nyxwell.app.core.error.AppError

data class PaywallUiState(
    val isLoadingPrice: Boolean = true,
    val price: String? = null,
    val isRestoring: Boolean = false,
)

sealed class PaywallUiEvent {
    data object RestorePurchasesClicked : PaywallUiEvent()
}

sealed class PaywallUiEffect {
    data class ShowSnackbar(val error: AppError) : PaywallUiEffect()
    data object PurchasesRestored : PaywallUiEffect()
}
