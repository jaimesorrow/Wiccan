package com.jaimesorrow.wiccan.data.repository

import com.google.firebase.functions.FirebaseFunctionsException
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.error.toAppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.core.time.LocalCalendarProvider
import com.jaimesorrow.wiccan.data.local.CachedDailyContentDao
import com.jaimesorrow.wiccan.data.mapper.toCachedEntity
import com.jaimesorrow.wiccan.data.mapper.toContent
import com.jaimesorrow.wiccan.data.mapper.toDomain
import com.jaimesorrow.wiccan.data.remote.ContentFunctionsDataSource
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.repository.ContentRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ContentRepositoryImpl @Inject constructor(
    private val functionsDataSource: ContentFunctionsDataSource,
    private val dao: CachedDailyContentDao,
    private val currentUserProvider: CurrentUserProvider,
    private val calendar: LocalCalendarProvider,
) : ContentRepository {

    private val refreshMutex = Mutex()

    override suspend fun refreshTodayContent(userId: String): AppResult<Content> = refreshMutex.withLock {
        try {
            if (currentUserProvider.currentUserId != userId) return AppResult.Failure(AppError.Unauthorized)
            val cached = dao.get()
            if (cached != null && (cached.userId != userId || cached.deliveredDate != calendar.today)) {
                dao.clear()
            }
            val payload = functionsDataSource.getTodayContent(calendar.timezone)
            val content = payload.toContent()
            val deliveredDate = payload["deliveredDate"] as? String
                ?: error("Missing server-assigned deliveredDate")
            val timezone = payload["timezone"] as? String
                ?: error("Missing server-assigned timezone")
            // An account switch while the callable is in flight must never cache another user's lesson.
            if (currentUserProvider.currentUserId != userId) return AppResult.Failure(AppError.Unauthorized)
            dao.replace(content.toCachedEntity(deliveredDate, userId, timezone))
            AppResult.Success(content)
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            val functionsError = t as? FirebaseFunctionsException
            val reason = (functionsError?.details as? Map<*, *>)?.get("reason") as? String
            val error = when (reason) {
                "NO_NEW_CONTENT" -> {
                    dao.clearForUser(userId)
                    AppError.NoNewLesson
                }
                "ASSIGNED_CONTENT_UNAVAILABLE" ->
                    AppError.Unknown("Today's assigned content is temporarily unavailable.")
                else -> t.toAppError()
            }
            AppResult.Failure(error)
        }
    }

    override fun observeTodayContent(): Flow<Content?> = dao.observe().map { entity ->
        val uid = currentUserProvider.currentUserId
        entity?.takeIf {
            uid != null && it.userId == uid && it.deliveredDate == calendar.today
        }?.toDomain()
    }
}
