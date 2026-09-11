package com.wiccan.app.ui.archive

import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.model.Content
import com.wiccan.app.domain.model.HistoryEntry
import com.wiccan.app.domain.repository.ArchiveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeArchiveRepository : ArchiveRepository {

    // No replay, unlike MutableStateFlow — mirrors the real Firestore
    // snapshot listener's genuine async gap before its first emission,
    // so tests can actually observe the pre-emission loading state.
    private val historyFlow = MutableSharedFlow<List<HistoryEntry>>(replay = 0, extraBufferCapacity = 1)
    var contentResults: MutableMap<String, AppResult<Content>> = mutableMapOf()

    override fun observeHistory(): Flow<List<HistoryEntry>> = historyFlow

    override suspend fun getContentById(contentId: String): AppResult<Content> =
        contentResults[contentId] ?: AppResult.Failure(AppError.Unknown("No fake result for '$contentId'"))

    fun emitHistory(entries: List<HistoryEntry>) {
        historyFlow.tryEmit(entries)
    }
}
