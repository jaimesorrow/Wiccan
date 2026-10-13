package com.jaimesorrow.wiccan.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.jaimesorrow.wiccan.core.config.FirebaseEmulatorConfig
import com.wiccan.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().also { firestore ->
        if (BuildConfig.USE_FIREBASE_EMULATORS) {
            firestore.useEmulator(FirebaseEmulatorConfig.HOST, FirebaseEmulatorConfig.FIRESTORE_PORT)
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance().also { functions ->
        if (BuildConfig.USE_FIREBASE_EMULATORS) {
            functions.useEmulator(FirebaseEmulatorConfig.HOST, FirebaseEmulatorConfig.FUNCTIONS_PORT)
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance().also { crashlytics ->
        if (BuildConfig.USE_FIREBASE_EMULATORS) {
            crashlytics.setCrashlyticsCollectionEnabled(false)
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context).also { analytics ->
            if (BuildConfig.USE_FIREBASE_EMULATORS) {
                analytics.setAnalyticsCollectionEnabled(false)
            }
        }
}
