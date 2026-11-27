package com.jaimesorrow.wiccan.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Streams server-verified access and closes it at the verified expiry.
 * Purchase verification, RTDN and reconciliation are the only writers;
 * a cached profile or client purchase callback never grants access.
 */
class UserTierDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    /** Server-mirrored state is informational only and never grants entitlement. */
    fun observeSubscriptionState(userId: String): Flow<SubscriptionState> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot?.metadata?.isFromCache != false) {
                    trySend(SubscriptionState.UNKNOWN)
                } else {
                    trySend(SubscriptionState.fromWire(snapshot.get(SUBSCRIPTION_STATE_FIELD) as? String))
                }
            }
        awaitClose { registration.remove() }
    }

    /** Informational only; a missing or cached product ID is treated as unknown. */
    fun observeProductId(userId: String): Flow<String?> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot?.metadata?.isFromCache != false) {
                    trySend(null)
                } else {
                    trySend((snapshot.get(PRODUCT_ID_FIELD) as? String)?.takeIf { it.isNotBlank() })
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeTier(userId: String): Flow<ContentTier> = callbackFlow {
        var expiryJob: Job? = null
        val verificationTimeout = launch {
            delay(10_000)
            trySend(ContentTier.FREE)
        }
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                expiryJob?.cancel()
                if (error != null) {
                    verificationTimeout.cancel()
                    trySend(ContentTier.FREE)
                    return@addSnapshotListener
                }
                // Disk cache alone never opens the app. Wait for a server
                // snapshot (or the bounded timeout) on a cold start.
                if (snapshot?.metadata?.isFromCache != false) {
                    trySend(ContentTier.FREE)
                    return@addSnapshotListener
                }
                verificationTimeout.cancel()
                val expiry = (snapshot.get("subscriptionExpiresAtMillis") as? Number)
                    ?.takeIf { it.toDouble().isFinite() }?.toLong() ?: 0L
                val remaining = expiry - System.currentTimeMillis()
                val active =
                    hasSubscriptionAccess(snapshot.get(TIER_FIELD) as? String, expiry, snapshot.get("deletionInProgress") == true)
                val tier = if (active) ContentTier.PREMIUM else ContentTier.FREE
                trySend(tier)
                if (active) {
                    expiryJob = launch {
                        delay(remaining.coerceAtLeast(0))
                        trySend(ContentTier.FREE)
                    }
                }
            }
        awaitClose {
            verificationTimeout.cancel()
            expiryJob?.cancel()
            registration.remove()
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val TIER_FIELD = "tier"
        const val SUBSCRIPTION_STATE_FIELD = "subscriptionState"
        const val PRODUCT_ID_FIELD = "productId"
    }
}

internal fun hasSubscriptionAccess(
    tier: String?,
    expiryMillis: Long,
    deleting: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
): Boolean =
    tier == "premium" && !deleting && expiryMillis > nowMillis
