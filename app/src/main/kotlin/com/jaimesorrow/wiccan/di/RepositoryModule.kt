package com.jaimesorrow.wiccan.di

import com.jaimesorrow.wiccan.data.repository.ArchiveRepositoryImpl
import com.jaimesorrow.wiccan.data.repository.AuthRepositoryImpl
import com.jaimesorrow.wiccan.data.repository.ContentRepositoryImpl
import com.jaimesorrow.wiccan.data.repository.NotificationRepositoryImpl
import com.jaimesorrow.wiccan.data.repository.SubscriptionRepositoryImpl
import com.jaimesorrow.wiccan.data.repository.TideRepositoryImpl
import com.jaimesorrow.wiccan.data.tides.NoaaTideDataSource
import com.jaimesorrow.wiccan.data.tides.TideDataSource
import com.jaimesorrow.wiccan.domain.repository.ArchiveRepository
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import com.jaimesorrow.wiccan.domain.repository.ContentRepository
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import com.jaimesorrow.wiccan.domain.repository.TideRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindArchiveRepository(impl: ArchiveRepositoryImpl): ArchiveRepository

    @Binds
    @Singleton
    abstract fun bindTideRepository(impl: TideRepositoryImpl): TideRepository

    @Binds
    @Singleton
    abstract fun bindTideDataSource(impl: NoaaTideDataSource): TideDataSource
}
