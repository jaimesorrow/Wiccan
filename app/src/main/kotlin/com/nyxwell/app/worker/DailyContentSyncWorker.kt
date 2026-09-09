package com.nyxwell.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.domain.repository.ContentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Refreshes the Room-cached daily content once per day, per the "Local
 * caching" design: populates [com.nyxwell.app.data.local.CachedDailyContentEntity]
 * so the UI (and later, the widget) can read fast, offline-friendly data.
 * Scheduled by the app's periodic WorkManager request (not set up in this
 * pass) with [KEY_USER_ID] as input data.
 */
@HiltWorker
class DailyContentSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val contentRepository: ContentRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
        return when (contentRepository.refreshTodayContent(userId)) {
            is AppResult.Success -> Result.success()
            is AppResult.Error -> Result.retry()
        }
    }

    companion object {
        const val KEY_USER_ID = "user_id"
        const val UNIQUE_WORK_NAME = "daily_content_sync"
    }
}
