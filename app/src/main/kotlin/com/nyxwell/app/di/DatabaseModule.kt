package com.nyxwell.app.di

import android.content.Context
import androidx.room.Room
import com.nyxwell.app.data.local.CachedDailyContentDao
import com.nyxwell.app.data.local.NyxwellDatabase
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
    fun provideNyxwellDatabase(@ApplicationContext context: Context): NyxwellDatabase =
        Room.databaseBuilder(context, NyxwellDatabase::class.java, NyxwellDatabase.DATABASE_NAME).build()

    @Provides
    fun provideCachedDailyContentDao(database: NyxwellDatabase): CachedDailyContentDao =
        database.cachedDailyContentDao()
}
