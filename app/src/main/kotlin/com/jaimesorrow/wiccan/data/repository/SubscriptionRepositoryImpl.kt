package com.jaimesorrow.wiccan.data.repository

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.billing.PREMIUM_MONTHLY_PRODUCT_ID
import com.jaimesorrow.wiccan.billing.PREMIUM_PRODUCT_IDS
import com.jaimesorrow.wiccan.billing.WiccanBillingClient
import com.jaimesorrow.wiccan.billing.formattedPrice
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.error.toAppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.remote.SubscriptionFunctionsDataSource
import com.jaimesorrow.wiccan.data.remote.UserTierDataSource
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
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

    private var lastObservedTier: ContentTier? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            billingClient.purchaseUpdates.collect { purchases ->
                purchases.forEach { purchase -> handlePurchase(purchase) }
            }
        }
        scope.launch {
            try {
                billingClient.queryOwnedSubscriptions().forEach { purchase -> handlePurchase(purchase) }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to query owned subscriptions on startup", t)
            }
        }
    }

    override suspend fun launchPurchaseFlow(activity: Activity): AppResult<Unit> = try {
        val result = billingClient.launchPurchaseFlow(activity, PREMIUM_MONTHLY_PRODUCT_ID)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(
                AppError.PurchaseVerificationFailed(
                    result.debugMessage.ifBlank { "Google Play could not start the purchase flow." },
                ),
            )
        }
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
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

        val productId = purchase.products.firstOrNull { it in PREMIUM_PRODUCT_IDS } ?: return

        try {
            functionsDataSource.verifyPurchase(
                purchaseToken = purchase.purchaseToken,
                productId = productId,
            )
            analyticsLogger.logSubscriptionStarted()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to verify/acknowledge purchase", t)
        }
    }

    private companion object {
        const val TAG = "SubscriptionRepository"
    }
}
