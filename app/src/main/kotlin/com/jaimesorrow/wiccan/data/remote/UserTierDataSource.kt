package com.jaimesorrow.wiccan.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.jaimesorrow.wiccan.domain.model.ContentTier
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
                val expiry = (snapshot.get("subscriptionExpiresAtMillis") as? Number)?.toLong() ?: 0L
                val remaining = expiry - System.currentTimeMillis()
                val active =
                    hasSubscriptionAccess(snapshot.getString(TIER_FIELD), expiry, snapshot.getBoolean("deletionInProgress") == true)
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
    }
}

internal fun hasSubscriptionAccess(
    tier: String?,
    expiryMillis: Long,
    deleting: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
): Boolean =
    tier == "premium" && !deleting && expiryMillis > nowMillis
