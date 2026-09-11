package com.wiccan.app.domain.repository

import com.wiccan.app.core.result.AppResult

/**
 * The only layer allowed to touch FCM/the device-registration Cloud
 * Functions. `users/{uid}` has no client write access in Firestore rules,
 * so the token and time zone always go through the server, never a
 * direct Firestore write.
 */
interface NotificationRepository {
    /** Registers [token] along with the device's current time zone. */
    suspend fun registerToken(token: String): AppResult<Unit>

    /** Fetches the current FCM token and registers it (with time zone) in one step. */
    suspend fun refreshAndRegisterToken(): AppResult<Unit>

    /** Best-effort: clears this device's server-side registration, e.g. before sign-out. */
    suspend fun unregisterToken(): AppResult<Unit>
}
