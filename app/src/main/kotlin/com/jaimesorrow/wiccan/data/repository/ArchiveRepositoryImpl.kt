package com.jaimesorrow.wiccan.data.repository

import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.error.toAppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.mapper.toContent
import com.jaimesorrow.wiccan.data.mapper.toHistoryEntry
import com.jaimesorrow.wiccan.data.remote.ArchiveFirestoreDataSource
import com.jaimesorrow.wiccan.data.remote.ContentNotFoundException
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import com.jaimesorrow.wiccan.domain.repository.ArchiveRepository
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
