package com.nyxwell.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nyxwell.app.worker.DailyContentWorker
import com.nyxwell.app.worker.NyxwellWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NyxwellApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var nyxwellWorkerFactory: NyxwellWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = nyxwellWorkerFactory.workManagerConfiguration

    override fun onCreate() {
        super.onCreate()
        scheduleDailyContentRefresh()
    }

    private fun scheduleDailyContentRefresh() {
        val request = PeriodicWorkRequestBuilder<DailyContentWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyContentWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
