package com.jaimesorrow.wiccan.data.repository

import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.error.toAppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.mapper.toContentOrNull
import com.jaimesorrow.wiccan.data.mapper.toHistoryEntries
import com.jaimesorrow.wiccan.data.remote.ArchiveFirestoreDataSource
import com.jaimesorrow.wiccan.data.remote.ContentNotFoundException
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import com.jaimesorrow.wiccan.domain.repository.ArchiveRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArchiveRepositoryImpl @Inject constructor(
    private val dataSource: ArchiveFirestoreDataSource,
) : ArchiveRepository {

    override fun observeHistory(): Flow<AppResult<List<HistoryEntry>>> =
        dataSource.observeHistory().map { result ->
            when (result) {
                // Unreadable rows (unknown type, missing fields) are skipped, never thrown.
                is AppResult.Success -> AppResult.Success(result.data.toHistoryEntries())
                is AppResult.Failure -> result
            }
        }

    override suspend fun getContentById(contentId: String): AppResult<Content> = try {
        dataSource.getContentById(contentId).toContentOrNull()
            ?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.UnsupportedContent)
    } catch (t: CancellationException) {
        throw t
    } catch (t: ContentNotFoundException) {
        AppResult.Failure(AppError.NotFound)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }
}
