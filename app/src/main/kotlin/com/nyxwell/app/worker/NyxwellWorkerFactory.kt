package com.nyxwell.app.worker

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import javax.inject.Inject

/**
 * Lets WorkManager create [DailyContentWorker] with its Hilt-injected
 * dependencies. Wired into [androidx.work.Configuration.Provider] on
 * [com.nyxwell.app.NyxwellApplication] via [workManagerConfiguration].
 */
class NyxwellWorkerFactory @Inject constructor(
    private val hiltWorkerFactory: HiltWorkerFactory,
) {
    val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()
}
