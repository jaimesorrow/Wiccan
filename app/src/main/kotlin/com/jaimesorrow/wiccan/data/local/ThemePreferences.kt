package com.jaimesorrow.wiccan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Persists the user's [WiccanThemeMode] choice. Purely local/presentation
 * state - no server round trip, unlike [NotificationPreferences]. Stored
 * as the enum's name; anything missing, blank, or from a since-removed
 * enum entry (a legacy/invalid value) falls back to
 * [WiccanThemeMode.DEFAULT] rather than crashing, via
 * [WiccanThemeMode.fromPersistedName].
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val themeMode: Flow<WiccanThemeMode> = context.themeDataStore.data.map {
        WiccanThemeMode.fromPersistedName(it[KEY_THEME_MODE])
    }

    suspend fun setThemeMode(mode: WiccanThemeMode) {
        context.themeDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
