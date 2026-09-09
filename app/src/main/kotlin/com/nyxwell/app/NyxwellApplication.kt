package com.nyxwell.app

import android.app.Application
import androidx.work.Configuration
import com.nyxwell.app.worker.NyxwellWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NyxwellApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var nyxwellWorkerFactory: NyxwellWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = nyxwellWorkerFactory.workManagerConfiguration
}
