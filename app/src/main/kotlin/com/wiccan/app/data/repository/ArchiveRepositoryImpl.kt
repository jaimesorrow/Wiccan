package com.wiccan.app.data.repository

import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.error.toAppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.data.mapper.toContent
import com.wiccan.app.data.mapper.toHistoryEntry
import com.wiccan.app.data.remote.ArchiveFirestoreDataSource
import com.wiccan.app.data.remote.ContentNotFoundException
import com.wiccan.app.domain.model.Content
import com.wiccan.app.domain.model.HistoryEntry
import com.wiccan.app.domain.repository.ArchiveRepository
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
    } catch (t: ContentNotFoundException) {
        AppResult.Failure(AppError.NotFound)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }
}
