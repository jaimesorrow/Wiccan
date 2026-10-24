package com.jaimesorrow.wiccan.domain.repository

import android.app.Activity
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import kotlinx.coroutines.flow.Flow

/**
 * The only layer allowed to touch the Billing Library or the tier stored
 * in Firestore. Legacy [ContentTier] values represent active (PREMIUM) or
 * inactive (FREE) access. Wiccan has one subscription and no free content plan.
 */
interface SubscriptionRepository {
    /** Launches the Play purchase UI and reports recoverable Play errors. */
    suspend fun launchPurchaseFlow(activity: Activity): AppResult<Unit>

    /**
     * The server-verified tier, synced from `users/{uid}.tier`. Never
     * backed by a raw Billing Library query — the client's own purchase
     * state is never trusted as the source of truth.
     */
    fun observeTier(): Flow<ContentTier>

    /** Current eligible monthly offer, including its actual trial and renewal price. */
    suspend fun getOffer(): AppResult<SubscriptionOffer>

    /**
     * Re-queries Play for subscriptions the signed-in Google account already
     * owns and re-verifies each through the server, for a user who
     * reinstalled or switched devices without a fresh purchase event.
     */
    suspend fun restorePurchases(): AppResult<Unit>
}
