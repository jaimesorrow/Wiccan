package com.wiccan.app.domain.repository

import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.model.Content
import kotlinx.coroutines.flow.Flow

/**
 * The only layer allowed to talk to Firestore/Cloud Functions directly.
 * ViewModels read through [observeTodayContent] and trigger refreshes
 * through [refreshTodayContent]; neither touches Firebase APIs directly.
 */
interface ContentRepository {
    suspend fun refreshTodayContent(userId: String): AppResult<Content>

    /** Backed by the Room cache, so it works offline once populated. */
    fun observeTodayContent(): Flow<Content?>
}
