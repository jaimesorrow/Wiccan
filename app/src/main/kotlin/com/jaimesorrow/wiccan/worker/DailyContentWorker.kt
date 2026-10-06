package com.jaimesorrow.wiccan.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.ContentRepository
import com.jaimesorrow.wiccan.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Refreshes the Room-cached daily content once per day, per the "Local
 * caching" design: populates [com.jaimesorrow.wiccan.data.local.CachedDailyContentEntity]
 * so the UI and the home-screen widget can read fast, offline-friendly
 * data. Scheduled as unique periodic work from [com.jaimesorrow.wiccan.WiccanApplication]
 * so duplicate refresh jobs can't accumulate.
 */
@HiltWorker
class DailyContentWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val contentRepository: ContentRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val widgetUpdater: WidgetUpdater,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // No signed-in user means there is no assignment to refresh.
        val userId = currentUserProvider.currentUserId ?: return Result.success()

        return when (val result = contentRepository.refreshTodayContent(userId)) {
            is AppResult.Success -> {
                widgetUpdater.updateAll(applicationContext)
                Result.success()
            }
            is AppResult.Failure -> if (result.error == AppError.NoNewLesson) {
                // Exhaustion is a valid server result; render the cleared cache and wait for the next scheduled day.
                widgetUpdater.updateAll(applicationContext)
                Result.success()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "daily_content_refresh"
    }
}
