package com.jaimesorrow.wiccan.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.InAppMessageParams
import com.android.billingclient.api.InAppMessageResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import com.jaimesorrow.wiccan.domain.model.YearlySwitchOffer
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
    private val recoveryUpdateChannel = Channel<Unit>(Channel.UNLIMITED)
    val recoveryUpdates: Flow<Unit> = recoveryUpdateChannel.receiveAsFlow()

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
        productDetails.requireUnchangedOffer(expectedOffer)

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

    /**
     * Replaces the caller's current monthly subscription with the yearly
     * plan, using [YEARLY_SWITCH_REPLACEMENT_MODE].
     */
    suspend fun launchPlanChangeFlow(
        activity: Activity,
        userId: String,
        expectedOffer: YearlySwitchOffer,
        oldPurchaseToken: String,
    ): BillingResult {
        val productDetails = queryProductDetails(PREMIUM_YEARLY_PRODUCT_ID)
        productDetails.requireUnchangedSwitchOffer(expectedOffer)

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
            .setSubscriptionUpdateParams(yearlySwitchUpdateParams(oldPurchaseToken))
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

    /** Lets Play show its transactional grace/hold payment-recovery message. */
    suspend fun showTransactionalMessage(activity: Activity): BillingResult {
        ensureConnected()
        val params = InAppMessageParams.newBuilder()
            .addInAppMessageCategoryToShow(InAppMessageParams.InAppMessageCategoryId.TRANSACTIONAL)
            .build()
        return client.showInAppMessages(activity, params) { result ->
            if (result.responseCode == InAppMessageResult.InAppMessageResponseCode.SUBSCRIPTION_STATUS_UPDATED) {
                recoveryUpdateChannel.trySend(Unit)
            }
        }
    }

    private fun BillingResult.requireOk(operation: String) {
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            throw PlayBillingException(
                "Failed to $operation ($responseCode): $debugMessage",
            )
        }
    }
}

/** Play product ID for each standalone plan. */
fun SubscriptionPlan.productId(): String = when (this) {
    SubscriptionPlan.MONTHLY -> PREMIUM_MONTHLY_PRODUCT_ID
    SubscriptionPlan.YEARLY -> PREMIUM_YEARLY_PRODUCT_ID
}

private fun planForProduct(productId: String): SubscriptionPlan? = when (productId) {
    PREMIUM_MONTHLY_PRODUCT_ID -> SubscriptionPlan.MONTHLY
    PREMIUM_YEARLY_PRODUCT_ID -> SubscriptionPlan.YEARLY
    else -> null
}

/**
 * Only the configured plan shape for this product (monthly P1M or yearly
 * P1Y, auto-renewing), optionally preceded by a seven-day free trial or a
 * discounted intro whose own offer tags include `winback`. Base-plan tags
 * are ignored for selection: a tagged base plan stays a regular plan, and
 * a base plan is never treated as a win-back offer even if it carries that
 * tag. Any other shape fails closed.
 *
 * The price is not pinned: the paywall shows and sells whatever price Play
 * returns, so a Play Console price change never hides a plan. The US base
 * prices in the Terms live in [TermsBasePrices]; see docs/V1_1_PLAN.md.
 */
internal fun ProductDetails.subscriptionOffer(): SubscriptionOffer? {
    val plan = planForProduct(productId) ?: return null
    val expectedPeriod = when (plan) {
        SubscriptionPlan.MONTHLY -> "P1M"
        SubscriptionPlan.YEARLY -> "P1Y"
    }
    return subscriptionOfferDetails.orEmpty().mapNotNull { offer ->
        val phases = offer.pricingPhases.pricingPhaseList
        val renewal = phases.lastOrNull() ?: return@mapNotNull null
        if (renewal.recurrenceMode != ProductDetails.RecurrenceMode.INFINITE_RECURRING ||
            renewal.billingPeriod != expectedPeriod || renewal.priceAmountMicros <= 0
        ) {
            return@mapNotNull null
        }
        val hasTrial = phases.size == 2 && phases.first().let { trial ->
            trial.priceAmountMicros == 0L && trial.billingPeriod in setOf("P7D", "P1W") &&
                (
                    (trial.recurrenceMode == ProductDetails.RecurrenceMode.FINITE_RECURRING && trial.billingCycleCount == 1) ||
                        (trial.recurrenceMode == ProductDetails.RecurrenceMode.NON_RECURRING && trial.billingCycleCount == 0)
                    )
        }
        // Win-back is identified only by this offer's own tags and offerId.
        // Play also puts base-plan tags in offerTags when offerId is null, so
        // a tagged base plan must still be accepted as the regular plan.
        val isWinBack = offer.offerId != null && offer.offerTags.contains(WIN_BACK_TAG) &&
            phases.size == 2 && isDiscountedIntro(phases.first(), renewal, expectedPeriod)
        if (!hasTrial && !isWinBack && (phases.size != 1 || offer.offerId != null)) {
            return@mapNotNull null
        }
        SubscriptionOffer(
            formattedPrice = renewal.formattedPrice,
            hasFreeTrial = hasTrial,
            offerToken = offer.offerToken,
            plan = plan,
            priceAmountMicros = renewal.priceAmountMicros,
            priceCurrencyCode = renewal.priceCurrencyCode,
            isWinBack = isWinBack,
            winBackFormattedPrice = if (isWinBack) phases.first().formattedPrice else null,
            winBackBillingPeriod = if (isWinBack) phases.first().billingPeriod else null,
        )
    }.sortedWith(compareByDescending<SubscriptionOffer> { it.hasFreeTrial }.thenByDescending { it.isWinBack }).firstOrNull()
}

private const val WIN_BACK_TAG = "winback"
private const val SWITCH_YEARLY_TAG = "switch-yearly"

/**
 * Google Play "Single payment" intro phases (the live $0.99 first-month
 * offers) arrive as [ProductDetails.RecurrenceMode.NON_RECURRING] with
 * billing period `P1M`, including on the yearly base plan. A same-period
 * finite recurring intro remains valid for older offer shapes.
 */
private fun isDiscountedIntro(
    intro: ProductDetails.PricingPhase,
    renewal: ProductDetails.PricingPhase,
    expectedRenewalPeriod: String,
): Boolean {
    if (intro.priceAmountMicros !in 1L until renewal.priceAmountMicros) return false
    if (intro.priceCurrencyCode != renewal.priceCurrencyCode) return false
    val finiteSamePeriod = intro.billingPeriod == expectedRenewalPeriod &&
        intro.recurrenceMode == ProductDetails.RecurrenceMode.FINITE_RECURRING &&
        intro.billingCycleCount == 1
    val firstMonthPromo = intro.billingPeriod == "P1M" && isSingleMonthIntro(intro)
    return finiteSamePeriod || firstMonthPromo
}

/** True for Play's Single payment (`NON_RECURRING`) or a one-cycle `P1M` finite intro. */
private fun isSingleMonthIntro(intro: ProductDetails.PricingPhase): Boolean =
    when (intro.recurrenceMode) {
        ProductDetails.RecurrenceMode.NON_RECURRING -> intro.billingCycleCount in 0..1
        ProductDetails.RecurrenceMode.FINITE_RECURRING -> intro.billingCycleCount == 1
        else -> false
    }

/**
 * Prefer the yearly offer tagged `switch-yearly` (its own tags, never a
 * base-plan tag). Fall back to the yearly base plan when that offer is
 * missing or the wrong shape. Trials are never selected: a plan change
 * is not a new-customer trial.
 */
internal fun ProductDetails.yearlySwitchOffer(): YearlySwitchOffer? {
    if (productId != PREMIUM_YEARLY_PRODUCT_ID) return null
    val offers = subscriptionOfferDetails.orEmpty()
    val tagged = offers.firstNotNullOfOrNull { offer ->
        if (offer.offerId == null || !offer.offerTags.contains(SWITCH_YEARLY_TAG)) {
            null
        } else {
            offer.toYearlySwitchOffer()
        }
    }
    if (tagged != null) return tagged
    return offers.firstNotNullOfOrNull { offer ->
        if (offer.offerId != null) null else offer.toYearlySwitchOffer()
    }
}

private fun ProductDetails.SubscriptionOfferDetails.toYearlySwitchOffer(): YearlySwitchOffer? {
    val phases = pricingPhases.pricingPhaseList
    val renewal = phases.lastOrNull() ?: return null
    if (renewal.recurrenceMode != ProductDetails.RecurrenceMode.INFINITE_RECURRING ||
        renewal.billingPeriod != "P1Y" || renewal.priceAmountMicros <= 0
    ) {
        return null
    }
    val intro = when {
        phases.size == 1 && offerId == null -> null
        phases.size == 2 && isDiscountedIntro(phases.first(), renewal, "P1Y") -> phases.first()
        else -> return null
    }
    return YearlySwitchOffer(
        offerToken = offerToken,
        yearlyFormattedPrice = renewal.formattedPrice,
        introFormattedPrice = intro?.formattedPrice,
        priceAmountMicros = renewal.priceAmountMicros,
        priceCurrencyCode = renewal.priceCurrencyCode,
    )
}

/**
 * [BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE]
 * is only allowed when the new plan costs more per unit of time. The yearly
 * plan is cheaper per month than the monthly plan, so Play would reject
 * that mode. [BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION]
 * applies the yearly plan immediately and credits unused monthly time by
 * moving the next billing date forward, which is the wording shown in Settings.
 */
internal const val YEARLY_SWITCH_REPLACEMENT_MODE =
    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION

internal fun yearlySwitchUpdateParams(
    oldPurchaseToken: String,
): BillingFlowParams.SubscriptionUpdateParams =
    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
        .setOldPurchaseToken(oldPurchaseToken)
        .setSubscriptionReplacementMode(YEARLY_SWITCH_REPLACEMENT_MODE)
        .build()

/** Require the fresh Play terms to match the offer the user saw. */
internal fun ProductDetails.requireUnchangedOffer(expected: SubscriptionOffer) {
    if (subscriptionOffer() != expected) {
        throw PlayBillingException("Your Google Play offer changed. Refresh the subscription offer and try again.")
    }
}

internal fun ProductDetails.requireUnchangedSwitchOffer(expected: YearlySwitchOffer) {
    if (yearlySwitchOffer() != expected) {
        throw PlayBillingException("Your Google Play offer changed. Refresh the subscription offer and try again.")
    }
}

/** Bind Play purchases to the Firebase account without sending the raw UID. */
internal fun obfuscatedAccountId(userId: String): String = MessageDigest.getInstance("SHA-256")
    .digest(userId.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
