package com.jaimesorrow.wiccan.ui.paywall

import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan

data class PaywallUiState(
    val isLoadingPrice: Boolean = true,
    /** Monthly renewal price from Play; null when the monthly offer could not be loaded. */
    val price: String? = null,
    val hasFreeTrial: Boolean = false,
    val isRestoring: Boolean = false,
    /** Yearly renewal price from Play; null hides the yearly plan and keeps the monthly-only paywall. */
    val yearlyPrice: String? = null,
    val yearlyHasFreeTrial: Boolean = false,
    /** Yearly price per month in Play's currency, e.g. "$3.33". */
    val yearlyMonthlyEquivalent: String? = null,
    /** Whole-percent saving against twelve monthly payments, e.g. 44. */
    val yearlySavingsPercent: Int? = null,
    val selectedPlan: SubscriptionPlan = SubscriptionPlan.MONTHLY,
    val subscriptionOnHold: Boolean = false,
    val monthlyWinBackPrice: String? = null,
    val yearlyWinBackPrice: String? = null,
    /** Play intro period for a yearly win-back offer, e.g. `P1M` or `P1Y`. */
    val yearlyWinBackBillingPeriod: String? = null,
) {
    val hasYearlyPlan: Boolean get() = price != null && yearlyPrice != null

    /** Renewal price of the plan the checkout button will buy. */
    val selectedPrice: String? get() = if (selectedPlan == SubscriptionPlan.YEARLY) yearlyPrice else price

    /** Whether the plan the checkout button will buy starts with a free trial for this account. */
    val selectedHasFreeTrial: Boolean
        get() = if (selectedPlan == SubscriptionPlan.YEARLY) yearlyHasFreeTrial else hasFreeTrial
}

sealed class PaywallUiEvent {
    data object RestorePurchasesClicked : PaywallUiEvent()
    data object RetryPriceClicked : PaywallUiEvent()
    data class PlanSelected(val plan: SubscriptionPlan) : PaywallUiEvent()
}

sealed class PaywallUiEffect {
    data class ShowSnackbar(val error: AppError) : PaywallUiEffect()
    data object PurchasesRestored : PaywallUiEffect()
}
