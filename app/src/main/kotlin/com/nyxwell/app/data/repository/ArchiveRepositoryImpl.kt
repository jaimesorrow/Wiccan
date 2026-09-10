package com.nyxwell.app.data.repository

import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.data.mapper.toContent
import com.nyxwell.app.data.mapper.toHistoryEntry
import com.nyxwell.app.data.remote.ArchiveFirestoreDataSource
import com.nyxwell.app.domain.model.Content
import com.nyxwell.app.domain.model.HistoryEntry
import com.nyxwell.app.domain.repository.ArchiveRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArchiveRepositoryImpl @Inject constructor(
    private val dataSource: ArchiveFirestoreDataSource,
) : ArchiveRepository {

    override fun observeHistory(): Flow<List<HistoryEntry>> =
        dataSource.observeHistory().map { rows -> rows.map { it.toHistoryEntry() } }

    override suspend fun getContentById(contentId: String): AppResult<Content> = try {
        AppResult.Success(dataSource.getContentById(contentId).toContent())
    } catch (t: Throwable) {
        AppResult.Error(t)
    }
}
