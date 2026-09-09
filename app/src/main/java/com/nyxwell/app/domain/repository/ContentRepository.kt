package com.nyxwell.app.domain.repository

import com.nyxwell.app.domain.model.Content
import com.nyxwell.app.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    suspend fun refreshTodayContent(userId: String): AppResult<Content>
    fun observeTodayContent(): Flow<Content?>
}
