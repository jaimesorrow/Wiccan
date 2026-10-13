package com.jaimesorrow.wiccan.di

import com.google.firebase.auth.FirebaseAuth
import com.jaimesorrow.wiccan.core.config.FirebaseEmulatorConfig
import com.wiccan.app.BuildConfig
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.auth.FirebaseCurrentUserProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthProvidesModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance().also { auth ->
        if (BuildConfig.USE_FIREBASE_EMULATORS) {
            auth.useEmulator(FirebaseEmulatorConfig.HOST, FirebaseEmulatorConfig.AUTH_PORT)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindsModule {

    @Binds
    @Singleton
    abstract fun bindCurrentUserProvider(impl: FirebaseCurrentUserProvider): CurrentUserProvider
}
