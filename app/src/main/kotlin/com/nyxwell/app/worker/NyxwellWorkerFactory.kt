package com.nyxwell.app.worker

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import javax.inject.Inject

/**
 * Lets WorkManager create [DailyContentSyncWorker] with its Hilt-injected
 * dependencies. Wired into [androidx.work.Configuration.Provider] on the
 * Application class once the app has a launch entry point (out of scope
 * for this pass) via [workManagerConfiguration].
 */
class NyxwellWorkerFactory @Inject constructor(
    private val hiltWorkerFactory: HiltWorkerFactory,
) {
    val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()
}
