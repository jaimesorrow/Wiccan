package com.nyxwell.app.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.nyxwell.app.data.local.AppDatabase
import com.nyxwell.app.data.local.CachedDailyContentDao
import com.nyxwell.app.data.repository.FirebaseContentRepository
import com.nyxwell.app.domain.repository.ContentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "wiccan.db").build()

    @Provides
    fun provideCachedDailyContentDao(database: AppDatabase): CachedDailyContentDao =
        database.cachedDailyContentDao()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance()

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindContentRepository(
        implementation: FirebaseContentRepository,
    ): ContentRepository
}
