package com.jaimesorrow.wiccan

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jaimesorrow.wiccan.appcheck.AppCheckProviderInstaller
import com.jaimesorrow.wiccan.worker.DailyContentWorker
import com.jaimesorrow.wiccan.worker.WiccanWorkerFactory
import com.wiccan.app.BuildConfig
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
        // Must run before any other Firebase call - App Check attaches its
        // token to every subsequent Functions/Firestore request. Which
        // provider this installs is decided per build variant (src/debug
        // vs src/release), not at runtime - see AppCheckProviderInstaller.
        // Local Firebase emulators do not provide App Check. This flag can
        // only be enabled in a debug build; release hardcodes it to false.
        if (!BuildConfig.USE_FIREBASE_EMULATORS) {
            appCheckProviderInstaller.install()
        }

        // The manifest removes WorkManager's default App Startup initializer
        // so the Hilt-aware worker factory is always used. Initialize it
        // explicitly before the first getInstance() call below.
        WorkManager.initialize(this, workManagerConfiguration)
        scheduleDailyContentRefresh()
    }

    private fun scheduleDailyContentRefresh() {
        val request = PeriodicWorkRequestBuilder<DailyContentWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyContentWorker.UNIQUE_WORK_NAME,
            // Replace the persisted worker class after the Kotlin package migration.
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
