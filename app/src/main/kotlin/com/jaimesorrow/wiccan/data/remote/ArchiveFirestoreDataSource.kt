package com.jaimesorrow.wiccan.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ArchiveFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val currentUserProvider: CurrentUserProvider,
) {
    fun observeHistory(): Flow<List<Map<String, Any?>>> = callbackFlow {
        val uid = currentUserProvider.currentUserId
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val registration = firestore.collection(HISTORY_COLLECTION)
            .whereEqualTo("userId", uid)
            .orderBy("deliveredDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map { it.data ?: emptyMap() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun getContentById(contentId: String): Map<String, Any?> {
        val doc = firestore.collection(CONTENT_COLLECTION).document(contentId).get().await()
        if (!doc.exists()) throw ContentNotFoundException(contentId)
        val content = (doc.data ?: emptyMap()) + ("id" to doc.id)
        if (content["requiresPotionDisclosure"] != true) return content
        val disclosures = firestore.collection(SETTINGS_COLLECTION).document(DISCLOSURES_DOCUMENT).get().await()
        return content + ("potionSafetyDisclosure" to disclosures.getString("potionSafetyDisclosure"))
    }

    private companion object {
        const val HISTORY_COLLECTION = "userContentHistory"
        const val CONTENT_COLLECTION = "content"
        const val SETTINGS_COLLECTION = "settings"
        const val DISCLOSURES_DOCUMENT = "disclosures"
    }
}

class ContentNotFoundException(contentId: String) : Exception("Content '$contentId' not found")
