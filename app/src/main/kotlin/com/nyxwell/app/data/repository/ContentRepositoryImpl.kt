package com.nyxwell.app.data.repository

import com.nyxwell.app.core.error.toAppError
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.data.local.CachedDailyContentDao
import com.nyxwell.app.data.mapper.toCachedEntity
import com.nyxwell.app.data.mapper.toContent
import com.nyxwell.app.data.mapper.toDomain
import com.nyxwell.app.data.remote.ContentFunctionsDataSource
import com.nyxwell.app.domain.model.Content
import com.nyxwell.app.domain.repository.ContentRepository
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
            AppResult.Failure(t.toAppError())
        }
    }

    override fun observeTodayContent(): Flow<Content?> = dao.observe().map { it?.toDomain() }
}
