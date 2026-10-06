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
import com.jaimesorrow.wiccan.billing.obfuscatedAccountId
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
import kotlinx.coroutines.CancellationException
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
    }

    override suspend fun launchPurchaseFlow(activity: Activity): AppResult<Unit> {
        val userId = currentUserProvider.currentUserId ?: return AppResult.Failure(AppError.Unauthorized)
        return try {
            val result = billingClient.launchPurchaseFlow(activity, userId, PREMIUM_MONTHLY_PRODUCT_ID)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(
                    AppError.PurchaseVerificationFailed(
                        result.debugMessage.ifBlank { "Google Play could not start the purchase flow." },
                    ),
                )
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            AppResult.Failure(t.toAppError())
        }
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
    } catch (t: CancellationException) {
        throw t
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun restorePurchases(): AppResult<Unit> {
        val userId = currentUserProvider.currentUserId ?: return AppResult.Failure(AppError.Unauthorized)
        return try {
            var verificationError: AppError? = null
            billingClient.queryOwnedSubscriptions().forEach { purchase ->
                try {
                    verifyPurchase(purchase, userId)
                } catch (t: CancellationException) {
                    throw t
                } catch (t: Throwable) {
                    // One stale or legacy token must not prevent acknowledgement
                    // of another legitimate purchase on the same Play account.
                    if (verificationError == null) verificationError = t.toAppError()
                }
            }
            verificationError?.let { AppResult.Failure(it) } ?: AppResult.Success(Unit)
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            AppResult.Failure(t.toAppError())
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        val userId = currentUserProvider.currentUserId ?: return
        try {
            verifyPurchase(purchase, userId)
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            // The next signed-in foreground resume or explicit restore retries.
            Log.e(TAG, "Failed to verify/acknowledge purchase", t)
        }
    }

    private suspend fun verifyPurchase(purchase: Purchase, userId: String) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val productId = purchase.products.firstOrNull { it in PREMIUM_PRODUCT_IDS } ?: return
        val accountId = purchase.accountIdentifiers?.obfuscatedAccountId
        if (accountId != null && accountId != obfuscatedAccountId(userId)) return

        functionsDataSource.verifyPurchase(
            purchaseToken = purchase.purchaseToken,
            productId = productId,
        )
        if (!purchase.isAcknowledged) analyticsLogger.logSubscriptionStarted()
    }

    private companion object {
        const val TAG = "SubscriptionRepository"
    }
}
