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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val PREMIUM_MONTHLY_PRODUCT_ID = "wiccan_premium_monthly"
const val PREMIUM_YEARLY_PRODUCT_ID = "wiccan_premium_yearly"

val PREMIUM_PRODUCT_IDS: Set<String> = setOf(
    PREMIUM_MONTHLY_PRODUCT_ID,
    PREMIUM_YEARLY_PRODUCT_ID,
)

class PlayBillingException(message: String) : IllegalStateException(message)

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

    private val connectionMutex = Mutex()

    private val listener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            _purchaseUpdates.tryEmit(purchases)
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(listener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    private suspend fun ensureConnected() {
        if (client.isReady) return

        connectionMutex.withLock {
            if (client.isReady) return@withLock

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
            result.requireOk("connect to Play Billing")
        }
    }

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

        val result = client.queryProductDetails(queryParams)
        result.billingResult.requireOk("query product '$productId'")
        return result.productDetailsList?.firstOrNull()
            ?: throw PlayBillingException("Product '$productId' is not available from Play.")
    }

    /**
     * Launches Play's purchase UI. A non-OK result is returned to the repository
     * so the UI can surface a recoverable error instead of crashing.
     */
    suspend fun launchPurchaseFlow(
        activity: Activity,
        productId: String = PREMIUM_MONTHLY_PRODUCT_ID,
    ): BillingResult {
        val productDetails = queryProductDetails(productId)
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: throw PlayBillingException("No subscription offer available for '$productId'.")

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

        return client.launchBillingFlow(activity, flowParams)
    }

    suspend fun queryOwnedSubscriptions(): List<Purchase> {
        ensureConnected()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = client.queryPurchasesAsync(params)
        result.billingResult.requireOk("query owned subscriptions")
        return result.purchasesList
    }

    suspend fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params).requireOk("acknowledge purchase")
    }

    private fun BillingResult.requireOk(operation: String) {
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            throw PlayBillingException(
                "Failed to $operation ($responseCode): $debugMessage",
            )
        }
    }
}

fun ProductDetails.formattedPrice(): String? =
    subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
