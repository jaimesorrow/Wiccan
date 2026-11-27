package com.jaimesorrow.wiccan.ui.paywall

import android.util.Log
import androidx.lifecycle.ViewModel
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class SubscriptionSetupViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {
    private val _reconciling = MutableStateFlow(true)
    val reconciling = _reconciling.asStateFlow()
    suspend fun refreshPurchases() {
        _reconciling.value = true
        try {
            when (val result = subscriptionRepository.restorePurchases()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> Log.w("SubscriptionSetup", "Purchase reconciliation failed: ${result.error}")
            }
        } finally {
            _reconciling.value = false
        }
    }
}
