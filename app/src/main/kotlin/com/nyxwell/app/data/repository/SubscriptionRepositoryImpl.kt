package com.nyxwell.app.data.repository

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.Purchase
import com.nyxwell.app.billing.NyxwellBillingClient
import com.nyxwell.app.billing.PREMIUM_MONTHLY_PRODUCT_ID
import com.nyxwell.app.core.auth.CurrentUserProvider
import com.nyxwell.app.data.remote.SubscriptionFunctionsDataSource
import com.nyxwell.app.data.remote.UserTierDataSource
import com.nyxwell.app.domain.model.ContentTier
import com.nyxwell.app.domain.repository.SubscriptionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val billingClient: NyxwellBillingClient,
    private val functionsDataSource: SubscriptionFunctionsDataSource,
    private val userTierDataSource: UserTierDataSource,
    private val currentUserProvider: CurrentUserProvider,
) : SubscriptionRepository {

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
        return userTierDataSource.observeTier(userId)
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PREMIUM_MONTHLY_PRODUCT_ID)) return

        try {
            functionsDataSource.verifyPurchase(
                purchaseToken = purchase.purchaseToken,
                productId = PREMIUM_MONTHLY_PRODUCT_ID,
            )
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
