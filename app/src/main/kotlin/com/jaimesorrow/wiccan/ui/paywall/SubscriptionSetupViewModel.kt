package com.jaimesorrow.wiccan.ui.paywall

import android.util.Log
import androidx.lifecycle.ViewModel
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SubscriptionSetupViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {
    suspend fun refreshPurchases() {
        when (val result = subscriptionRepository.restorePurchases()) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> Log.w("SubscriptionSetup", "Purchase reconciliation failed: ${result.error}")
        }
    }
}
