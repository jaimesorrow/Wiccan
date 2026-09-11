package com.nyxwell.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
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
        installAppCheckProvider()
        scheduleDailyContentRefresh()
    }

    /**
     * Must run before any other Firebase call — App Check attaches its
     * token to every subsequent Functions/Firestore request. Play Integrity
     * requires a real, Play-signed install, so debug builds use the debug
     * provider instead (registered per-install via the Firebase console).
     */
    private fun installAppCheckProvider() {
        val factory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        Firebase.appCheck.installAppCheckProviderFactory(factory)
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
