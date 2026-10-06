package com.jaimesorrow.wiccan.billing

import android.app.Activity
import android.content.Context
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
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
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
 * and reports purchases. Entitlement and acknowledgement are handled server-side.
 */
@Singleton
class WiccanBillingClient @Inject constructor(
    @ApplicationContext context: Context,
) {
    // A purchase callback can arrive before the repository starts collecting.
    // Keep it queued until server verification runs instead of dropping it.
    private val purchaseUpdateChannel = Channel<List<Purchase>>(Channel.UNLIMITED)
    val purchaseUpdates: Flow<List<Purchase>> = purchaseUpdateChannel.receiveAsFlow()

    private val connectionMutex = Mutex()

    private val listener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchaseUpdateChannel.trySend(purchases)
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
        userId: String,
        productId: String = PREMIUM_MONTHLY_PRODUCT_ID,
    ): BillingResult {
        val productDetails = queryProductDetails(productId)
        val offerToken = productDetails.recurringBasePlan()?.offerToken
            ?: throw PlayBillingException("No recurring base plan available for '$productId'.")

        val flowParams = BillingFlowParams.newBuilder()
            .setObfuscatedAccountId(obfuscatedAccountId(userId))
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

    private fun BillingResult.requireOk(operation: String) {
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            throw PlayBillingException(
                "Failed to $operation ($responseCode): $debugMessage",
            )
        }
    }
}

/** Choose the same regular base plan for the displayed price and Play UI. */
internal fun ProductDetails.recurringBasePlan(): ProductDetails.SubscriptionOfferDetails? {
    val period = if (productId == PREMIUM_YEARLY_PRODUCT_ID) "P1Y" else "P1M"
    return subscriptionOfferDetails?.firstOrNull { offer ->
        offer.offerId == null && offer.pricingPhases.pricingPhaseList.any { phase ->
            phase.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING &&
                phase.billingPeriod == period
        }
    }
}

fun ProductDetails.formattedPrice(): String? = recurringBasePlan()?.pricingPhases?.pricingPhaseList
    ?.lastOrNull { it.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING }
    ?.formattedPrice

/** Bind Play purchases to the Firebase account without sending the raw UID. */
internal fun obfuscatedAccountId(userId: String): String = MessageDigest.getInstance("SHA-256")
    .digest(userId.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
