package com.wiccan.app.data.repository

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.Purchase
import com.wiccan.app.analytics.AnalyticsLogger
import com.wiccan.app.billing.PREMIUM_MONTHLY_PRODUCT_ID
import com.wiccan.app.billing.WiccanBillingClient
import com.wiccan.app.billing.formattedPrice
import com.wiccan.app.core.auth.CurrentUserProvider
import com.wiccan.app.core.error.toAppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.data.remote.SubscriptionFunctionsDataSource
import com.wiccan.app.data.remote.UserTierDataSource
import com.wiccan.app.domain.model.ContentTier
import com.wiccan.app.domain.repository.SubscriptionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val billingClient: WiccanBillingClient,
    private val functionsDataSource: SubscriptionFunctionsDataSource,
    private val userTierDataSource: UserTierDataSource,
    private val currentUserProvider: CurrentUserProvider,
    private val analyticsLogger: AnalyticsLogger,
) : SubscriptionRepository {

    // Tracks the last-observed tier per user so a PREMIUM -> FREE transition
    // can be attributed to the scheduled lapsed-subscription re-check — the
    // only code path that ever downgrades an existing premium user's tier.
    private var lastObservedTier: ContentTier? = null

    // Owned for the process lifetime: the Play purchase result can arrive
    // from the billing UI after the screen that launched it (and its
    // ViewModel scope) is already gone.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            billingClient.purchaseUpdates.collect { purchases ->
                purchases.forEach { purchase -> handlePurchase(purchase) }
            }
        }
        // Catches purchases made while the app wasn't running, or whose
        // verification/acknowledgement was interrupted last session.
        scope.launch {
            try {
                billingClient.queryOwnedSubscriptions().forEach { purchase -> handlePurchase(purchase) }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to query owned subscriptions on startup", t)
            }
        }
    }

    override suspend fun launchPurchaseFlow(activity: Activity) {
        billingClient.launchPurchaseFlow(activity, PREMIUM_MONTHLY_PRODUCT_ID)
    }

    override fun observeTier(): Flow<ContentTier> {
        val userId = currentUserProvider.currentUserId ?: return flowOf(ContentTier.FREE)
        return userTierDataSource.observeTier(userId).onEach { tier ->
            if (lastObservedTier == ContentTier.PREMIUM && tier == ContentTier.FREE) {
                analyticsLogger.logSubscriptionLapsed()
            }
            lastObservedTier = tier
        }
    }

    override suspend fun getFormattedPrice(): AppResult<String> = try {
        val price = billingClient.queryProductDetails(PREMIUM_MONTHLY_PRODUCT_ID).formattedPrice()
            ?: error("No price available for '$PREMIUM_MONTHLY_PRODUCT_ID'.")
        AppResult.Success(price)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun restorePurchases(): AppResult<Unit> = try {
        billingClient.queryOwnedSubscriptions().forEach { purchase -> handlePurchase(purchase) }
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PREMIUM_MONTHLY_PRODUCT_ID)) return

        try {
            functionsDataSource.verifyPurchase(
                purchaseToken = purchase.purchaseToken,
                productId = PREMIUM_MONTHLY_PRODUCT_ID,
            )
            analyticsLogger.logSubscriptionStarted()
            billingClient.acknowledgePurchase(purchase)
        } catch (t: Throwable) {
            // No UI is wired to surface this yet (out of scope for this pass);
            // an un-acknowledged purchase is safely retried on the next
            // purchaseUpdates emission or app restart.
            Log.e(TAG, "Failed to verify/acknowledge purchase", t)
        }
    }

    private companion object {
        const val TAG = "SubscriptionRepository"
    }
}
