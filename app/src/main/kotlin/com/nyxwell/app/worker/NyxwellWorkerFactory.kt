package com.nyxwell.app.worker

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import javax.inject.Inject

/**
 * Lets WorkManager create [DailyContentSyncWorker] with its Hilt-injected
 * dependencies. Exposed via [workManagerConfiguration] and wired by
 * [com.nyxwell.app.NyxwellApplication], which implements
 * [androidx.work.Configuration.Provider].
 */
class NyxwellWorkerFactory @Inject constructor(
    private val hiltWorkerFactory: HiltWorkerFactory,
) {
    val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()
}
