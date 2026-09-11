package com.wiccan.app.di

import android.content.Context
import androidx.room.Room
import com.wiccan.app.data.local.CachedDailyContentDao
import com.wiccan.app.data.local.WiccanDatabase
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
        Room.databaseBuilder(context, WiccanDatabase::class.java, WiccanDatabase.DATABASE_NAME).build()

    @Provides
    fun provideCachedDailyContentDao(database: WiccanDatabase): CachedDailyContentDao =
        database.cachedDailyContentDao()
}
