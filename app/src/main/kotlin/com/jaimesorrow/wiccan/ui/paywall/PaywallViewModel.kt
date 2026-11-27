package com.jaimesorrow.wiccan.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.billing.yearlyMonthlyEquivalent
import com.jaimesorrow.wiccan.billing.yearlySavingsPercent
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
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

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val analyticsLogger: AnalyticsLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = _state.asStateFlow()

    private val effectChannel = Channel<PaywallUiEffect>(Channel.BUFFERED)

    /** Set once the user taps a plan, so a refreshed offer never overrides their choice. */
    private var userPickedPlan = false
    val effects: Flow<PaywallUiEffect> = effectChannel.receiveAsFlow()

    init {
        analyticsLogger.logPaywallViewed()
        loadPrice()
        viewModelScope.launch {
            subscriptionRepository.observeSubscriptionState().collect { subscriptionState ->
                _state.update { it.copy(subscriptionOnHold = subscriptionState == SubscriptionState.ON_HOLD) }
            }
        }
    }

    fun onEvent(event: PaywallUiEvent) {
        when (event) {
            PaywallUiEvent.RestorePurchasesClicked -> restorePurchases()
            PaywallUiEvent.RetryPriceClicked -> loadPrice()
            is PaywallUiEvent.PlanSelected -> selectPlan(event.plan)
        }
    }

    private fun selectPlan(plan: SubscriptionPlan) {
        if (plan == SubscriptionPlan.YEARLY && !_state.value.hasYearlyPlan) return
        userPickedPlan = true
        _state.update { it.copy(selectedPlan = plan) }
    }

    /** True while a resume refresh runs in the background with the current offers still shown. */
    private var refreshing = false

    /**
     * A returning subscriber may no longer be eligible for the trial shown last
     * time, so offers are re-read on resume. The plans already on screen stay
     * put while that runs (no spinner, no flash, no scroll jump); they are
     * replaced only when Play answers. A failed refresh keeps them silently.
     */
    fun refreshOffer() {
        val current = _state.value
        if (current.isLoadingPrice || refreshing) return
        if (current.price == null) {
            loadPrice()
            return
        }
        refreshing = true
        // The visible offer is intentionally kept on screen, but it is no
        // longer purchasable until the refreshed Play terms are committed.
        // This prevents checkout against a new cache while the old trial or
        // price is still being advertised.
        _state.update { it.copy(isLoadingPrice = true) }
        viewModelScope.launch {
            try {
                val monthly = (subscriptionRepository.getOffer() as? AppResult.Success)?.data ?: run {
                    // Keep the last verified terms visible if Play cannot be
                    // reached, and make them purchasable again; the cache was
                    // not replaced on a failed lookup.
                    _state.update { it.copy(isLoadingPrice = false) }
                    return@launch
                }
                val yearly = (subscriptionRepository.getYearlyOffer() as? AppResult.Success)?.data
                applyOffers(monthly, yearly)
            } finally {
                refreshing = false
            }
        }
    }

    fun subscribe(activity: Activity) {
        viewModelScope.launch {
            val plan = _state.value.let { if (it.hasYearlyPlan) it.selectedPlan else SubscriptionPlan.MONTHLY }
            when (val result = subscriptionRepository.launchPurchaseFlow(activity, plan)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> {
                    effectChannel.send(PaywallUiEffect.ShowSnackbar(result.error))
                    loadPrice()
                }
            }
        }
    }

    /**
     * Loads the monthly offer exactly as before, then the yearly offer. A
     * missing yearly plan only hides it; the monthly checkout still works.
     * Yearly is preselected as the best value whenever both are available,
     * unless the user already picked monthly on this screen.
     */
    private fun loadPrice() {
        _state.update {
            it.copy(
                isLoadingPrice = true,
                price = null,
                hasFreeTrial = false,
                yearlyPrice = null,
                yearlyHasFreeTrial = false,
                yearlyMonthlyEquivalent = null,
                yearlySavingsPercent = null,
            )
        }
        viewModelScope.launch {
            when (val result = subscriptionRepository.getOffer()) {
                is AppResult.Success -> {
                    val yearly = (subscriptionRepository.getYearlyOffer() as? AppResult.Success)?.data
                    applyOffers(result.data, yearly)
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(isLoadingPrice = false) }
                    effectChannel.send(PaywallUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }

    private fun applyOffers(monthly: SubscriptionOffer, yearly: SubscriptionOffer?) {
        _state.update {
            it.copy(
                isLoadingPrice = false,
                price = monthly.formattedPrice,
                hasFreeTrial = monthly.hasFreeTrial,
                yearlyPrice = yearly?.formattedPrice,
                yearlyHasFreeTrial = yearly?.hasFreeTrial == true,
                yearlyMonthlyEquivalent = yearly?.let(::yearlyMonthlyEquivalent),
                yearlySavingsPercent = yearly?.let { offer -> yearlySavingsPercent(offer, monthly) },
                monthlyWinBackPrice = monthly.winBackFormattedPrice,
                yearlyWinBackPrice = yearly?.winBackFormattedPrice,
                selectedPlan = when {
                    yearly == null -> SubscriptionPlan.MONTHLY
                    userPickedPlan -> it.selectedPlan
                    else -> SubscriptionPlan.YEARLY
                },
            )
        }
    }

    private fun restorePurchases() {
        _state.update { it.copy(isRestoring = true) }
        viewModelScope.launch {
            when (val result = subscriptionRepository.restorePurchases()) {
                is AppResult.Success -> {
                    _state.update { it.copy(isRestoring = false) }
                    effectChannel.send(PaywallUiEffect.PurchasesRestored)
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(isRestoring = false) }
                    effectChannel.send(PaywallUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }
}
