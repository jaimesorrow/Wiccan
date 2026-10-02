package com.jaimesorrow.wiccan.worker

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import javax.inject.Inject

/**
 * Lets WorkManager create [DailyContentWorker] with its Hilt-injected
 * dependencies. Wired into [androidx.work.Configuration.Provider] on
 * [com.jaimesorrow.wiccan.WiccanApplication] via [workManagerConfiguration].
 */
class WiccanWorkerFactory @Inject constructor(
    private val hiltWorkerFactory: HiltWorkerFactory,
) {
    val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()
}
