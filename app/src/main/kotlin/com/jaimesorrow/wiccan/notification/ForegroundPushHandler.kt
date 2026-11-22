package com.jaimesorrow.wiccan.notification

import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/** How long to wait for the stored reminder choice before giving up (and not posting). */
private const val PREFERENCE_READ_TIMEOUT_MS = 2_000L

/**
 * Decides whether a push received in the foreground should be shown. A push
 * can be in flight when the user signs out or turns the daily reminder off,
 * so it is only posted while someone is signed in and that account's reminder
 * is on ([NotificationRepository.notificationsEnabled] is false when signed out).
 */
class ForegroundPushHandler @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val poster: ForegroundNotificationPoster,
) {
    /** Returns true when a notification was posted. */
    suspend fun handle(content: ForegroundNotificationContent): Boolean {
        if (currentUserProvider.currentUserId == null) return false
        val enabled = withTimeoutOrNull(PREFERENCE_READ_TIMEOUT_MS) {
            notificationRepository.notificationsEnabled
                .catch { t -> if (t is CancellationException) throw t else emit(false) }
                .firstOrNull()
        } ?: false
        if (!enabled || currentUserProvider.currentUserId == null) return false
        return poster.post(content)
    }
}
