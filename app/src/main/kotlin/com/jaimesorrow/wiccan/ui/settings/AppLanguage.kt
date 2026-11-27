package com.jaimesorrow.wiccan.ui.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import com.wiccan.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Languages explicitly supported by the app. An empty tag follows the device language. */
enum class AppLanguage(val languageTag: String, @StringRes val labelRes: Int) {
    SYSTEM("", R.string.settings_language_system),
    ENGLISH("en", R.string.settings_language_english),
    SPANISH("es", R.string.settings_language_spanish),
    PORTUGUESE_BRAZIL("pt-BR", R.string.settings_language_portuguese_brazil),
    ;

    companion object {
        fun fromLanguageTag(tag: String?): AppLanguage {
            val normalized = tag.orEmpty().trim().replace('_', '-')
            if (normalized.isEmpty()) return SYSTEM
            entries.firstOrNull { it.languageTag.equals(normalized, ignoreCase = true) }?.let { return it }
            return when (normalized.substringBefore('-').lowercase(Locale.ROOT)) {
                "en" -> ENGLISH
                "es" -> SPANISH
                "pt" -> PORTUGUESE_BRAZIL
                else -> SYSTEM
            }
        }
    }
}

/**
 * Small compatibility layer for per-app language selection. Android 13+ uses the platform locale
 * manager (and therefore also appears in system App language settings); older releases persist the
 * selection locally and receive a localized configuration when the activity is recreated.
 *
 * A language chosen on Android 12 or earlier is migrated into [LocaleManager] once after an upgrade,
 * and is not reapplied if the user later changes the app language in system settings.
 */
object AppLanguageManager {
    private const val PREFS = "app_language"
    private const val KEY_LANGUAGE_TAG = "language_tag"
    private const val KEY_MIGRATED_TO_LOCALE_MANAGER = "migrated_to_locale_manager"

    fun selected(context: Context): AppLanguage {
        migrateLegacyPreference(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            return AppLanguage.fromLanguageTag(if (locales.isEmpty) "" else locales[0].toLanguageTag())
        }
        return AppLanguage.fromLanguageTag(preferences(context).getString(KEY_LANGUAGE_TAG, ""))
    }

    /**
     * The locale actually in use: a concrete supported tag (`en`, `es`, or `pt-BR`), never the
     * empty "follow the device" value. Respects a pre-Android 13 override rather than reading
     * only the system locale.
     */
    fun resolvedLanguageTag(context: Context): String {
        val selected = selected(context)
        if (selected != AppLanguage.SYSTEM) return selected.languageTag
        val matched = AppLanguage.fromLanguageTag(deviceLanguageTag(context))
        return if (matched == AppLanguage.SYSTEM) "en" else matched.languageTag
    }

    fun select(activity: Activity, language: AppLanguage) {
        preferences(activity).edit()
            .putString(KEY_LANGUAGE_TAG, language.languageTag)
            .putBoolean(KEY_MIGRATED_TO_LOCALE_MANAGER, true)
            .apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (language == AppLanguage.SYSTEM) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(language.languageTag)
                }
        } else {
            activity.recreate()
        }
    }

    fun localizedContext(base: Context): Context {
        migrateLegacyPreference(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val language = selected(base)
        if (language == AppLanguage.SYSTEM) return base
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language.languageTag))
        return base.createConfigurationContext(configuration)
    }

    /**
     * Copies a pre-Android 13 SharedPreferences choice into [LocaleManager] the first time the
     * app runs on Android 13+. Does nothing if the platform list is already set, so a later
     * choice made in system settings is left alone.
     */
    fun migrateLegacyPreference(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val prefs = preferences(context)
        if (prefs.getBoolean(KEY_MIGRATED_TO_LOCALE_MANAGER, false)) return
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val legacyTag = prefs.getString(KEY_LANGUAGE_TAG, "").orEmpty()
        if (localeManager.applicationLocales.isEmpty && legacyTag.isNotEmpty()) {
            localeManager.applicationLocales = LocaleList.forLanguageTags(legacyTag)
        }
        prefs.edit().putBoolean(KEY_MIGRATED_TO_LOCALE_MANAGER, true).apply()
    }

    private fun deviceLanguageTag(context: Context): String {
        val configuration = context.resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (configuration.locales.isEmpty) {
                Locale.getDefault().toLanguageTag()
            } else {
                configuration.locales[0].toLanguageTag()
            }
        } else {
            @Suppress("DEPRECATION")
            configuration.locale.toLanguageTag()
        }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Resolves the per-app locale for Cloud Function payloads. */
@Singleton
class AppLocaleProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun languageTag(): String = AppLanguageManager.resolvedLanguageTag(context)
}
