package com.wiccan.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.wiccan.app.appcheck.AppCheckProviderInstaller
import com.wiccan.app.worker.DailyContentWorker
import com.wiccan.app.worker.WiccanWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class WiccanApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var wiccanWorkerFactory: WiccanWorkerFactory

    @Inject
    lateinit var appCheckProviderInstaller: AppCheckProviderInstaller

    override val workManagerConfiguration: Configuration
        get() = wiccanWorkerFactory.workManagerConfiguration

    override fun onCreate() {
        super.onCreate()
        appCheckProviderInstaller.install()
        // The manifest removes WorkManagerInitializer so Hilt's WorkerFactory
        // can be supplied. Initialize explicitly before getInstance().
        WorkManager.initialize(this, workManagerConfiguration)
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
