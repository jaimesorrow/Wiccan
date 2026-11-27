package com.jaimesorrow.wiccan.domain.repository

import android.app.Activity
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
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
     * Re-queries Play for subscriptions the signed-in Google account already
     * owns and re-verifies each through the server, for a user who
     * reinstalled or switched devices without a fresh purchase event.
     */
    suspend fun restorePurchases(): AppResult<Unit>
}
