package com.nyxwell.app.ui.paywall

import android.app.Activity
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.domain.model.ContentTier
import com.nyxwell.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSubscriptionRepository : SubscriptionRepository {

    private val tierFlow = MutableStateFlow(ContentTier.FREE)
    var launchCalls: MutableList<Activity> = mutableListOf()
    var priceResult: AppResult<String> = AppResult.Success("$4.99/mo")
    var restoreResult: AppResult<Unit> = AppResult.Success(Unit)
    var restoreCallCount: Int = 0

    override suspend fun launchPurchaseFlow(activity: Activity) {
        launchCalls += activity
    }

    override fun observeTier(): Flow<ContentTier> = tierFlow

    override suspend fun getFormattedPrice(): AppResult<String> = priceResult

    override suspend fun restorePurchases(): AppResult<Unit> {
        restoreCallCount++
        return restoreResult
    }

    fun emitTier(tier: ContentTier) {
        tierFlow.value = tier
    }
}
