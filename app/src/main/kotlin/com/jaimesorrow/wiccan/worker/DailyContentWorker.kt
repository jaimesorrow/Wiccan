package com.jaimesorrow.wiccan.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.ContentRepository
import com.jaimesorrow.wiccan.widget.WiccanWidget
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
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // No signed-in user yet (auth isn't wired up until a later pass) —
        // nothing to refresh, and that's not a worker failure.
        val userId = currentUserProvider.currentUserId ?: return Result.success()

        return when (contentRepository.refreshTodayContent(userId)) {
            is AppResult.Success -> {
                WiccanWidget().updateAll(applicationContext)
                Result.success()
            }
            is AppResult.Failure -> Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "daily_content_refresh"
    }
}
