package com.jaimesorrow.wiccan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.analyticsConsentDataStore: DataStore<Preferences> by preferencesDataStore(name = "analytics_consent")

/**
 * The user's opt-in to usage analytics and crash reports. Device-local by
 * design: it governs what this device's Firebase SDKs collect, and it
 * defaults to false so nothing is collected until the user says yes.
 */
@Singleton
class AnalyticsConsentPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val analyticsConsent: Flow<Boolean> = context.analyticsConsentDataStore.data.map {
        it[KEY_ANALYTICS_CONSENT] ?: false
    }

    suspend fun setAnalyticsConsent(granted: Boolean) {
        context.analyticsConsentDataStore.edit { it[KEY_ANALYTICS_CONSENT] = granted }
    }

    private companion object {
        val KEY_ANALYTICS_CONSENT = booleanPreferencesKey("analytics_consent")
    }
}
