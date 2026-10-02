package com.jaimesorrow.wiccan.data.repository

import com.google.firebase.functions.FirebaseFunctionsException
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.error.toAppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.local.CachedDailyContentDao
import com.jaimesorrow.wiccan.data.mapper.toCachedEntity
import com.jaimesorrow.wiccan.data.mapper.toContent
import com.jaimesorrow.wiccan.data.mapper.toDomain
import com.jaimesorrow.wiccan.data.remote.ContentFunctionsDataSource
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.repository.ContentRepository
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContentRepositoryImpl @Inject constructor(
    private val functionsDataSource: ContentFunctionsDataSource,
    private val dao: CachedDailyContentDao,
) : ContentRepository {

    override suspend fun refreshTodayContent(userId: String): AppResult<Content> {
        return try {
            val content = functionsDataSource.getTodayContent().toContent()
            dao.replace(content.toCachedEntity(deliveredDate = LocalDate.now(ZoneOffset.UTC).toString()))
            AppResult.Success(content)
        } catch (t: Throwable) {
            val error = if ((t as? FirebaseFunctionsException)?.code ==
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED
            ) {
                AppError.NoNewLesson
            } else {
                t.toAppError()
            }
            AppResult.Failure(error)
        }
    }

    override fun observeTodayContent(): Flow<Content?> = dao.observe().map { it?.toDomain() }
}
