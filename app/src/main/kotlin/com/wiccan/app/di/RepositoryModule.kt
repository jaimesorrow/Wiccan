package com.wiccan.app.di

import com.wiccan.app.data.repository.ArchiveRepositoryImpl
import com.wiccan.app.data.repository.AuthRepositoryImpl
import com.wiccan.app.data.repository.ContentRepositoryImpl
import com.wiccan.app.data.repository.NotificationRepositoryImpl
import com.wiccan.app.data.repository.SubscriptionRepositoryImpl
import com.wiccan.app.domain.repository.ArchiveRepository
import com.wiccan.app.domain.repository.AuthRepository
import com.wiccan.app.domain.repository.ContentRepository
import com.wiccan.app.domain.repository.NotificationRepository
import com.wiccan.app.domain.repository.SubscriptionRepository
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
}
