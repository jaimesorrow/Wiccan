package com.jaimesorrow.wiccan.domain.repository

import com.jaimesorrow.wiccan.core.result.AppResult
import kotlinx.coroutines.flow.Flow

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

    /** Local cache of the toggle state for Settings to display - defaults to true. */
    val notificationsEnabled: Flow<Boolean>

    /** Toggles the daily push for this device on the server, without unregistering it. */
    suspend fun setNotificationsEnabled(enabled: Boolean): AppResult<Unit>
}
