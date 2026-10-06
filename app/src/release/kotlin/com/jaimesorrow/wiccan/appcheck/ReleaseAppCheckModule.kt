package com.jaimesorrow.wiccan.appcheck

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Release builds are real, Play-signed installs, so Play Integrity attestation is available. */
class ReleaseAppCheckProviderInstaller : AppCheckProviderInstaller {
    override fun install() {
        Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ReleaseAppCheckModule {
    @Provides
    @Singleton
    fun provideAppCheckProviderInstaller(): AppCheckProviderInstaller = ReleaseAppCheckProviderInstaller()
}
