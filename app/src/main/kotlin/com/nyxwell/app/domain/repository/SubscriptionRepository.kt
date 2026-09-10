package com.nyxwell.app.domain.repository

import android.app.Activity
import com.nyxwell.app.domain.model.ContentTier
import kotlinx.coroutines.flow.Flow

/**
 * The only layer allowed to touch the Billing Library or the tier stored
 * in Firestore. [ContentTier] is reused here rather than a separate "user
 * tier" enum — a subscriber's tier and the content tiers it unlocks are
 * the same concept.
 */
interface SubscriptionRepository {
    /** Launches the Play purchase UI for the single premium subscription. */
    suspend fun launchPurchaseFlow(activity: Activity)

    /**
     * The server-verified tier, synced from `users/{uid}.tier`. Never
     * backed by a raw Billing Library query — the client's own purchase
     * state is never trusted as the source of truth.
     */
    fun observeTier(): Flow<ContentTier>
}
