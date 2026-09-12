package com.wiccan.app.appcheck

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Debug builds use the debug provider - Play Integrity requires a real, Play-signed install. */
class DebugAppCheckProviderInstaller : AppCheckProviderInstaller {
    override fun install() {
        Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DebugAppCheckModule {
    @Provides
    @Singleton
    fun provideAppCheckProviderInstaller(): AppCheckProviderInstaller = DebugAppCheckProviderInstaller()
}
