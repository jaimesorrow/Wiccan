package com.jaimesorrow.wiccan.ui.paywall

import android.app.Activity
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
import com.jaimesorrow.wiccan.domain.model.YearlySwitchOffer
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSubscriptionRepository : SubscriptionRepository {

    private val tierFlow = MutableStateFlow(ContentTier.FREE)
    private val stateFlow = MutableStateFlow(SubscriptionState.UNKNOWN)
    private val productIdFlow = MutableStateFlow<String?>(null)
    var launchCalls: MutableList<Activity> = mutableListOf()
    var launchedPlans: MutableList<SubscriptionPlan> = mutableListOf()

    /** Null keeps the monthly-only paywall, as when Play has no yearly plan configured. */
    var yearlyOffer: SubscriptionOffer? = null
    var yearlyCallCount: Int = 0
    var yearlySwitchOffer: YearlySwitchOffer? = null
    var yearlySwitchCallCount: Int = 0
    var switchLaunchCallCount: Int = 0
    var launchResult: AppResult<Unit> = AppResult.Success(Unit)
    var switchLaunchResult: AppResult<Unit> = AppResult.Success(Unit)
    var priceResult: AppResult<String> = AppResult.Success("$5.99")
    var hasFreeTrial: Boolean = true
    var monthlyWinBackPrice: String? = null
    var priceCallCount: Int = 0

    /** When set, getOffer() suspends until it completes, to observe the screen mid-refresh. */
    var offerGate: CompletableDeferred<Unit>? = null
    var restoreResult: AppResult<Unit> = AppResult.Success(Unit)
    var restoreCallCount: Int = 0

    override suspend fun launchPurchaseFlow(activity: Activity, plan: SubscriptionPlan): AppResult<Unit> {
        launchCalls += activity
        launchedPlans += plan
        return launchResult
    }

    var transactionalMessageResult: AppResult<Unit> = AppResult.Success(Unit)
    var transactionalMessageCalls: MutableList<Activity> = mutableListOf()

    override fun observeTier(): Flow<ContentTier> = tierFlow
    override fun observeSubscriptionState(): Flow<SubscriptionState> = stateFlow
    override fun observeProductId(): Flow<String?> = productIdFlow
    override suspend fun showTransactionalMessage(activity: Activity): AppResult<Unit> {
        transactionalMessageCalls += activity
        return transactionalMessageResult
    }

    override suspend fun getOffer(): AppResult<SubscriptionOffer> {
        priceCallCount++
        offerGate?.await()
        return when (val result = priceResult) {
            is AppResult.Success -> AppResult.Success(
                SubscriptionOffer(
                    result.data,
                    hasFreeTrial,
                    "offer-token",
                    SubscriptionPlan.MONTHLY,
                    5_990_000L,
                    "USD",
                    isWinBack = monthlyWinBackPrice != null,
                    winBackFormattedPrice = monthlyWinBackPrice,
                ),
            )
            is AppResult.Failure -> result
        }
    }

    override suspend fun getYearlyOffer(): AppResult<SubscriptionOffer?> {
        yearlyCallCount++
        return AppResult.Success(yearlyOffer)
    }

    override suspend fun getYearlySwitchOffer(): AppResult<YearlySwitchOffer?> {
        yearlySwitchCallCount++
        return AppResult.Success(yearlySwitchOffer)
    }

    override suspend fun launchSwitchToYearly(activity: Activity): AppResult<Unit> {
        switchLaunchCallCount++
        launchCalls += activity
        return switchLaunchResult
    }

    override suspend fun restorePurchases(): AppResult<Unit> {
        restoreCallCount++
        return restoreResult
    }

    fun emitTier(tier: ContentTier) {
        tierFlow.value = tier
    }

    fun emitSubscriptionState(state: SubscriptionState) {
        stateFlow.value = state
    }

    fun emitProductId(productId: String?) {
        productIdFlow.value = productId
    }
}
