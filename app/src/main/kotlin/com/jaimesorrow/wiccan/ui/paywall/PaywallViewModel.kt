package com.jaimesorrow.wiccan.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.core.result.AppResult
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
    val effects: Flow<PaywallUiEffect> = effectChannel.receiveAsFlow()

    init {
        analyticsLogger.logPaywallViewed()
        loadPrice()
    }

    fun onEvent(event: PaywallUiEvent) {
        when (event) {
            PaywallUiEvent.RestorePurchasesClicked -> restorePurchases()
        }
    }

    fun subscribe(activity: Activity) {
        viewModelScope.launch {
            when (val result = subscriptionRepository.launchPurchaseFlow(activity)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> effectChannel.send(PaywallUiEffect.ShowSnackbar(result.error))
            }
        }
    }

    private fun loadPrice() {
        viewModelScope.launch {
            when (val result = subscriptionRepository.getFormattedPrice()) {
                is AppResult.Success -> _state.update { it.copy(isLoadingPrice = false, price = result.data) }
                is AppResult.Failure -> {
                    _state.update { it.copy(isLoadingPrice = false) }
                    effectChannel.send(PaywallUiEffect.ShowSnackbar(result.error))
                }
            }
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
