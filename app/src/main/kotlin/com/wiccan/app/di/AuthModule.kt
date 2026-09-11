package com.wiccan.app.di

import com.google.firebase.auth.FirebaseAuth
import com.wiccan.app.core.auth.CurrentUserProvider
import com.wiccan.app.core.auth.FirebaseCurrentUserProvider
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
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindsModule {

    @Binds
    @Singleton
    abstract fun bindCurrentUserProvider(impl: FirebaseCurrentUserProvider): CurrentUserProvider
}
