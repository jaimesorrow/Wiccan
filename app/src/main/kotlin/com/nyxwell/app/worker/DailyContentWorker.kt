package com.nyxwell.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nyxwell.app.core.auth.CurrentUserProvider
import androidx.glance.appwidget.updateAll
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.domain.repository.ContentRepository
import com.nyxwell.app.widget.NyxwellWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Refreshes the Room-cached daily content once per day, per the "Local
 * caching" design: populates [com.nyxwell.app.data.local.CachedDailyContentEntity]
 * so the UI and the home-screen widget can read fast, offline-friendly
 * data. Scheduled as unique periodic work from [com.nyxwell.app.NyxwellApplication]
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
                NyxwellWidget().updateAll(applicationContext)
                Result.success()
            }
            is AppResult.Error -> Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "daily_content_refresh"
    }
}
