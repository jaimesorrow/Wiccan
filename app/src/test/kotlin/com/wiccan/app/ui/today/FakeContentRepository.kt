package com.wiccan.app.ui.today

import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.model.Content
import com.wiccan.app.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory ContentRepository double for TodayViewModel tests. The test
 * configures the next refresh outcome via [nextResult], and can push
 * cache updates by calling [emitCached].
 */
class FakeContentRepository : ContentRepository {

    private val cache = MutableStateFlow<Content?>(null)

    var nextResult: AppResult<Content> = AppResult.Failure(AppError.Unknown("Not configured"))
    var refreshCalls: MutableList<String> = mutableListOf()

    override suspend fun refreshTodayContent(userId: String): AppResult<Content> {
        refreshCalls += userId
        val result = nextResult
        if (result is AppResult.Success) {
            cache.value = result.data
        }
        return result
    }

    override fun observeTodayContent(): Flow<Content?> = cache

    fun emitCached(content: Content?) {
        cache.value = content
    }

    /** Test-only peek at the current cached value, without collecting the Flow. */
    fun cachedNow(): Content? = cache.value
}
