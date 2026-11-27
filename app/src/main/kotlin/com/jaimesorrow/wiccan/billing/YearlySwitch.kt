package com.jaimesorrow.wiccan.billing

import com.android.billingclient.api.Purchase
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionState

/**
 * True for an active monthly subscriber who may be offered a yearly switch.
 * Grace, hold, pause, and other recovery states are excluded: those
 * subscriptions already have a failed payment, and a replacement flow
 * can fail or charge a declined instrument.
 */
fun isYearlySwitchEligible(
    tier: ContentTier,
    subscriptionState: SubscriptionState,
    productId: String?,
): Boolean =
    tier == ContentTier.PREMIUM &&
        subscriptionState == SubscriptionState.ACTIVE &&
        productId == PREMIUM_MONTHLY_PRODUCT_ID

fun canShowYearlySwitch(
    tier: ContentTier,
    subscriptionState: SubscriptionState,
    productId: String?,
    switchOfferAvailable: Boolean,
): Boolean = isYearlySwitchEligible(tier, subscriptionState, productId) && switchOfferAvailable

/**
 * The monthly purchase token to replace, if and only if it belongs to the
 * signed-in Wiccan account. A Google account can own more than one monthly
 * subscription; never pick the first one Play returns.
 *
 * A purchase belongs to this account when its obfuscated account id matches,
 * or, for older purchases that have no account id, when the token is the
 * one the server already verified for this user.
 */
internal fun monthlySwitchPurchaseToken(
    purchases: List<Purchase>,
    expectedAccountId: String,
    serverPurchaseToken: String?,
): String? = purchases.firstOrNull { purchase ->
    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
        PREMIUM_MONTHLY_PRODUCT_ID in purchase.products &&
        monthlyPurchaseBelongsToAccount(purchase, expectedAccountId, serverPurchaseToken)
}?.purchaseToken

internal fun monthlyPurchaseBelongsToAccount(
    purchase: Purchase,
    expectedAccountId: String,
    serverPurchaseToken: String?,
): Boolean {
    val accountId = purchase.accountIdentifiers?.obfuscatedAccountId
    if (accountId != null) return accountId == expectedAccountId
    return !serverPurchaseToken.isNullOrBlank() && purchase.purchaseToken == serverPurchaseToken
}
