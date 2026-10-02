package com.jaimesorrow.wiccan.di

import android.content.Context
import androidx.room.Room
import com.jaimesorrow.wiccan.data.local.CachedDailyContentDao
import com.jaimesorrow.wiccan.data.local.WICCAN_DATABASE_MIGRATIONS
import com.jaimesorrow.wiccan.data.local.WiccanDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWiccanDatabase(@ApplicationContext context: Context): WiccanDatabase =
        Room.databaseBuilder(context, WiccanDatabase::class.java, WiccanDatabase.DATABASE_NAME)
            .addMigrations(*WICCAN_DATABASE_MIGRATIONS)
            .build()

    @Provides
    fun provideCachedDailyContentDao(database: WiccanDatabase): CachedDailyContentDao =
        database.cachedDailyContentDao()
}
