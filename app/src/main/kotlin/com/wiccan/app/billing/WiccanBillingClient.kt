package com.wiccan.app.billing

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

/** Wiccan's single subscription product — no pricing ladder, per spec. */
const val PREMIUM_MONTHLY_PRODUCT_ID = "wiccan_premium_monthly"

/**
 * Thin wrapper around Play Billing Library 9.1.0. Entitlement remains
 * server-authoritative; this class only discovers offers, launches Play UI,
 * reports purchases, and acknowledges purchases after server verification.
 */
@Singleton
class WiccanBillingClient @Inject constructor(
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
        // PBL 9 removed the no-arg enablePendingPurchases API. Google's
        // migration guidance uses enableOneTimeProducts() for the required
        // PendingPurchasesParams opt-in; Wiccan still sells subscriptions only.
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        // PBL 8+ can reconnect automatically before API calls, reducing
        // transient SERVICE_DISCONNECTED failures.
        .enableAutoServiceReconnection()
        .build()

    private suspend fun ensureConnected() {
        if (client.isReady) return
        val result = suspendCancellableCoroutine<BillingResult> { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (continuation.isActive) continuation.resume(billingResult)
                }

                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) {
                        continuation.resume(
                            BillingResult.newBuilder()
                                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                                .setDebugMessage("Billing service disconnected during setup")
                                .build(),
                        )
                    }
                }
            })
        }
        check(result.responseCode == BillingClient.BillingResponseCode.OK) {
            "Play Billing connection failed (${result.responseCode}): ${result.debugMessage}"
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

        val result = client.launchBillingFlow(activity, flowParams)
        check(result.responseCode == BillingClient.BillingResponseCode.OK) {
            "Play Billing flow failed (${result.responseCode}): ${result.debugMessage}"
        }
    }

    /** Re-query active purchases on startup/restore; entitlement is still server verified. */
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
        val result = client.acknowledgePurchase(params)
        check(result.responseCode == BillingClient.BillingResponseCode.OK) {
            "Purchase acknowledgement failed (${result.responseCode}): ${result.debugMessage}"
        }
    }
}

/** The subscription's localized recurring price, e.g. "$4.99/mo". */
fun ProductDetails.formattedPrice(): String? =
    subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
