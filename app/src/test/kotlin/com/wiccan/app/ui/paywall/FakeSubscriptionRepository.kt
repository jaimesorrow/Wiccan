package com.wiccan.app.ui.paywall

import android.app.Activity
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.model.ContentTier
import com.wiccan.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSubscriptionRepository : SubscriptionRepository {

    private val tierFlow = MutableStateFlow(ContentTier.FREE)
    var launchCalls: MutableList<Activity> = mutableListOf()
    var launchResult: AppResult<Unit> = AppResult.Success(Unit)
    var priceResult: AppResult<String> = AppResult.Success("$4.99/mo")
    var restoreResult: AppResult<Unit> = AppResult.Success(Unit)
    var restoreCallCount: Int = 0

    override suspend fun launchPurchaseFlow(activity: Activity): AppResult<Unit> {
        launchCalls += activity
        return launchResult
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
