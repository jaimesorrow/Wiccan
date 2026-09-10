package com.nyxwell.app.ui.subscription

import android.app.Activity
import com.nyxwell.app.domain.model.ContentTier
import com.nyxwell.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSubscriptionRepository : SubscriptionRepository {

    private val tierFlow = MutableStateFlow(ContentTier.FREE)
    var launchCalls: MutableList<Activity> = mutableListOf()

    override suspend fun launchPurchaseFlow(activity: Activity) {
        launchCalls += activity
    }

    override fun observeTier(): Flow<ContentTier> = tierFlow

    fun emitTier(tier: ContentTier) {
        tierFlow.value = tier
    }
}
