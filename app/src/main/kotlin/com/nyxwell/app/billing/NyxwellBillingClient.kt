package com.nyxwell.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/** Nyxwell's single subscription product — no pricing ladder, per spec. */
const val PREMIUM_MONTHLY_PRODUCT_ID = "nyxwell_premium_monthly"

/**
 * Thin wrapper around the Play Billing Library. Owns the [BillingClient]
 * connection for the process lifetime and republishes purchase results as
 * a [Flow] so callers don't need to implement [PurchasesUpdatedListener]
 * themselves. Business logic (verification, acknowledgement sequencing,
 * tier sync) lives in [com.nyxwell.app.data.repository.SubscriptionRepositoryImpl]
 * — this class only talks to the Billing Library.
 */
@Singleton
class NyxwellBillingClient @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val _purchaseUpdates = MutableSharedFlow<List<Purchase>>(extraBufferCapacity = 1)
    val purchaseUpdates: Flow<List<Purchase>> = _purchaseUpdates

    private val listener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            _purchaseUpdates.tryEmit(purchases)
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(listener)
        // Subscriptions get pending-purchase support by default; the
        // no-arg opt-in only matters for one-time products, which Nyxwell
        // doesn't sell.
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().build())
        .build()

    private suspend fun ensureConnected() {
        if (client.isReady) return
        suspendCancellableCoroutine { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    // Reconnection is attempted lazily the next time a call needs the client.
                }
            })
        }
    }

    /** Queries Play for the current, localized [ProductDetails] for [productId]. */
    suspend fun queryProductDetails(productId: String = PREMIUM_MONTHLY_PRODUCT_ID): ProductDetails {
        ensureConnected()

        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()

        return client.queryProductDetails(queryParams).productDetailsList?.firstOrNull()
            ?: error("Product '$productId' is not available from Play.")
    }

    /** Queries product details and launches the Play purchase UI for [productId]. */
    suspend fun launchPurchaseFlow(activity: Activity, productId: String = PREMIUM_MONTHLY_PRODUCT_ID) {
        val productDetails = queryProductDetails(productId)
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: error("No subscription offer available for '$productId'.")

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()

        client.launchBillingFlow(activity, flowParams)
    }

    /**
     * Purchases made while the app wasn't running, or whose verification/
     * acknowledgement was interrupted, don't arrive via [purchaseUpdates].
     * Google's guidance is to re-query on every startup to catch those.
     */
    suspend fun queryOwnedSubscriptions(): List<Purchase> {
        ensureConnected()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        return client.queryPurchasesAsync(params).purchasesList
    }

    /** Play requires acknowledgement within 3 days or the purchase is auto-refunded. */
    suspend fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params)
    }
}

/** The subscription's localized recurring price, e.g. "$4.99/mo". */
fun ProductDetails.formattedPrice(): String? =
    subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
