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
import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
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
        expectedOffer: SubscriptionOffer,
    ): BillingResult {
        val productDetails = queryProductDetails(productId)
        // Requery rather than caching ProductDetails. Never silently replace a
        // displayed free trial with an immediate charge if eligibility changed.
        if (productDetails.subscriptionOffer() != expectedOffer) {
            throw PlayBillingException("Your Google Play offer changed. Refresh the subscription offer and try again.")
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setObfuscatedAccountId(obfuscatedAccountId(userId))
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(expectedOffer.offerToken)
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

/** Only the configured monthly plan, optionally preceded by a seven-day trial. */
internal fun ProductDetails.subscriptionOffer(): SubscriptionOffer? {
    if (productId != PREMIUM_MONTHLY_PRODUCT_ID) return null
    return subscriptionOfferDetails.orEmpty().mapNotNull { offer ->
        val phases = offer.pricingPhases.pricingPhaseList
        val renewal = phases.lastOrNull() ?: return@mapNotNull null
        if (renewal.recurrenceMode != ProductDetails.RecurrenceMode.INFINITE_RECURRING ||
            renewal.billingPeriod != "P1M" || renewal.priceAmountMicros <= 0 ||
            (renewal.priceCurrencyCode == "USD" && renewal.priceAmountMicros != 5_990_000L)
        ) {
            return@mapNotNull null
        }
        val hasTrial = phases.size == 2 && phases.first().let { trial ->
            trial.priceAmountMicros == 0L && trial.billingPeriod == "P7D" &&
                trial.recurrenceMode == ProductDetails.RecurrenceMode.FINITE_RECURRING && trial.billingCycleCount == 1
        }
        if (!hasTrial && (phases.size != 1 || offer.offerId != null)) return@mapNotNull null
        SubscriptionOffer(renewal.formattedPrice, hasTrial, offer.offerToken)
    }.sortedByDescending { it.hasFreeTrial }.firstOrNull()
}

/** Bind Play purchases to the Firebase account without sending the raw UID. */
internal fun obfuscatedAccountId(userId: String): String = MessageDigest.getInstance("SHA-256")
    .digest(userId.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
