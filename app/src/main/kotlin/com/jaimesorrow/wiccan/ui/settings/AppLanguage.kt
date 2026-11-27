package com.jaimesorrow.wiccan.ui.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import com.wiccan.app.R
import java.util.Locale

/** Languages explicitly supported by the app. An empty tag follows the device language. */
enum class AppLanguage(val languageTag: String, @StringRes val labelRes: Int) {
    SYSTEM("", R.string.settings_language_system),
    SPANISH("es", R.string.settings_language_spanish),
    PORTUGUESE_BRAZIL("pt-BR", R.string.settings_language_portuguese_brazil),
    ;

    companion object {
        fun fromLanguageTag(tag: String?): AppLanguage = entries.firstOrNull {
            it.languageTag.equals(tag.orEmpty(), ignoreCase = true)
        } ?: SYSTEM
    }
}

/**
 * Small compatibility layer for per-app language selection. Android 13+ uses the platform locale
 * manager (and therefore also appears in system App language settings); older releases persist the
 * selection locally and receive a localized configuration when the activity is recreated.
 */
object AppLanguageManager {
    private const val PREFS = "app_language"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    fun selected(context: Context): AppLanguage {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            return AppLanguage.fromLanguageTag(if (locales.isEmpty) "" else locales[0].toLanguageTag())
        }
        return AppLanguage.fromLanguageTag(preferences(context).getString(KEY_LANGUAGE_TAG, ""))
    }

    fun select(activity: Activity, language: AppLanguage) {
        preferences(activity).edit().putString(KEY_LANGUAGE_TAG, language.languageTag).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (language == AppLanguage.SYSTEM) LocaleList.getEmptyLocaleList()
                else LocaleList.forLanguageTags(language.languageTag)
        } else {
            activity.recreate()
        }
    }

    fun localizedContext(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val language = selected(base)
        if (language == AppLanguage.SYSTEM) return base
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language.languageTag))
        return base.createConfigurationContext(configuration)
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
