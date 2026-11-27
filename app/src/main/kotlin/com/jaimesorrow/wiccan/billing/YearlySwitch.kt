package com.jaimesorrow.wiccan.billing

import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionState

/**
 * The Settings "Switch to yearly" row is shown only to an active monthly
 * subscriber. Grace, hold, pause, and other recovery states are hidden:
 * those subscriptions already have a failed payment, and a replacement
 * flow that credits remaining paid time can fail or charge a declined
 * instrument. Yearly and free accounts never see the row.
 */
fun canShowYearlySwitch(
    tier: ContentTier,
    subscriptionState: SubscriptionState,
    productId: String?,
    switchOfferAvailable: Boolean,
): Boolean =
    tier == ContentTier.PREMIUM &&
        subscriptionState == SubscriptionState.ACTIVE &&
        productId == PREMIUM_MONTHLY_PRODUCT_ID &&
        switchOfferAvailable
