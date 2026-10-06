package com.jaimesorrow.wiccan.ui.paywall

import android.app.Activity
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSubscriptionRepository : SubscriptionRepository {

    private val tierFlow = MutableStateFlow(ContentTier.FREE)
    var launchCalls: MutableList<Activity> = mutableListOf()
    var launchResult: AppResult<Unit> = AppResult.Success(Unit)
    var priceResult: AppResult<String> = AppResult.Success("$4.99/mo")
    var priceCallCount: Int = 0
    var restoreResult: AppResult<Unit> = AppResult.Success(Unit)
    var restoreCallCount: Int = 0

    override suspend fun launchPurchaseFlow(activity: Activity): AppResult<Unit> {
        launchCalls += activity
        return launchResult
    }

    override fun observeTier(): Flow<ContentTier> = tierFlow

    override suspend fun getFormattedPrice(): AppResult<String> {
        priceCallCount++
        return priceResult
    }

    override suspend fun restorePurchases(): AppResult<Unit> {
        restoreCallCount++
        return restoreResult
    }

    fun emitTier(tier: ContentTier) {
        tierFlow.value = tier
    }
}
