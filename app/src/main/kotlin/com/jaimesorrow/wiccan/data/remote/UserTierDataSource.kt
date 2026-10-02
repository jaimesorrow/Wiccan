package com.jaimesorrow.wiccan.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.jaimesorrow.wiccan.domain.model.ContentTier
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Streams `users/{uid}.tier`. This — and the daily lapse re-check — are
 * the only writers of that field (enforced by Firestore rules); the app
 * only ever reads it.
 */
class UserTierDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    fun observeTier(userId: String): Flow<ContentTier> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(ContentTier.FREE)
                    return@addSnapshotListener
                }
                val tier = if (snapshot?.getString(TIER_FIELD) == "premium") {
                    ContentTier.PREMIUM
                } else {
                    ContentTier.FREE
                }
                trySend(tier)
            }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val TIER_FIELD = "tier"
    }
}
