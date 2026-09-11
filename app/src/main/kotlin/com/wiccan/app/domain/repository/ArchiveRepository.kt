package com.wiccan.app.domain.repository

import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.model.Content
import com.wiccan.app.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * The only layer allowed to query `userContentHistory`/`content` directly
 * for Archive display. Unlike the daily-selection logic, reading a user's
 * own already-delivered history is safe as a direct Firestore query —
 * Firestore rules already scope it to the signed-in user's own rows, and
 * there's no "seen" state left to game by reading it.
 */
interface ArchiveRepository {
    /** Newest first. */
    fun observeHistory(): Flow<List<HistoryEntry>>

    suspend fun getContentById(contentId: String): AppResult<Content>
}
