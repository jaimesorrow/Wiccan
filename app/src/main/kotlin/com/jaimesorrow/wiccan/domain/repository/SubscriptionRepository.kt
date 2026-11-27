package com.jaimesorrow.wiccan.domain.repository

import android.app.Activity
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
import com.jaimesorrow.wiccan.domain.model.YearlySwitchOffer
import kotlinx.coroutines.flow.Flow

/**
 * The only layer allowed to touch the Billing Library or the tier stored
 * in Firestore. Legacy [ContentTier] values represent active (PREMIUM) or
 * inactive (FREE) access. Wiccan Premium is sold as two standalone plans
 * (monthly and yearly) and there is no free content plan.
 */
interface SubscriptionRepository {
    /**
     * Launches the Play purchase UI for [plan], using the offer last loaded
     * for it, and reports recoverable Play errors.
     */
    suspend fun launchPurchaseFlow(activity: Activity, plan: SubscriptionPlan = SubscriptionPlan.MONTHLY): AppResult<Unit>

    /**
     * The server-verified tier, synced from `users/{uid}.tier`. Never
     * backed by a raw Billing Library query — the client's own purchase
     * state is never trusted as the source of truth.
     */
    fun observeTier(): Flow<ContentTier>

    /** Informational server-mirrored Play state; never used to grant access. */
    fun observeSubscriptionState(): Flow<SubscriptionState>

    /** Server-mirrored Play product ID, or null when unknown. Never used to grant access. */
    fun observeProductId(): Flow<String?>

    /** Emitted when a Play purchase sheet closes without a new purchase. */
    fun observePurchaseFlowDismissed(): Flow<Unit>

    /** Requests Play's own transactional payment-recovery message. */
    suspend fun showTransactionalMessage(activity: Activity): AppResult<Unit>

    /** Current eligible monthly offer, including its actual trial and renewal price. */
    suspend fun getOffer(): AppResult<SubscriptionOffer>

    /**
     * Current eligible yearly offer, or `Success(null)` when the yearly plan is
     * not available from Play (not configured yet, or wrong terms). The paywall
     * then falls back to the monthly plan alone.
     */
    suspend fun getYearlyOffer(): AppResult<SubscriptionOffer?>

    /**
     * Yearly offer used when an active monthly subscriber switches plans.
     * Prefers the Play offer tagged `switch-yearly`, otherwise the yearly
     * base plan. `Success(null)` when the yearly product is unavailable.
     */
    suspend fun getYearlySwitchOffer(): AppResult<YearlySwitchOffer?>

    /**
     * Launches Play's subscription replacement UI from monthly to yearly,
     * using the switch offer last loaded for this account.
     */
    suspend fun launchSwitchToYearly(activity: Activity): AppResult<Unit>

    /**
     * Re-queries Play for subscriptions the signed-in Google account already
     * owns and re-verifies each through the server, for a user who
     * reinstalled or switched devices without a fresh purchase event.
     */
    suspend fun restorePurchases(): AppResult<Unit>
}
