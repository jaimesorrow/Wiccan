package com.wiccan.app.ui.paywall

import android.util.Log
import androidx.lifecycle.ViewModel
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.repository.SubscriptionRepository
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
