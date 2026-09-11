package com.wiccan.app.domain.repository

import com.wiccan.app.core.result.AppResult

/**
 * The only layer allowed to touch FCM/the registerFcmToken Cloud Function.
 * `users/{uid}` has no client write access in Firestore rules, so the
 * token always goes through the server, never a direct Firestore write.
 */
interface NotificationRepository {
    suspend fun registerToken(token: String): AppResult<Unit>

    /** Fetches the current FCM token and registers it in one step. */
    suspend fun refreshAndRegisterToken(): AppResult<Unit>
}
