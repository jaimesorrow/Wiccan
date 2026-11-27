package com.jaimesorrow.wiccan.ui.settings

import android.app.Activity
import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppLanguageManagerTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `fromLanguageTag matches explicit tags and language prefixes`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("en-US"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLanguageTag("es-MX"))
        assertEquals(AppLanguage.PORTUGUESE_BRAZIL, AppLanguage.fromLanguageTag("pt_BR"))
        assertEquals(AppLanguage.PORTUGUESE_BRAZIL, AppLanguage.fromLanguageTag("pt"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("fr"))
    }

    @Test
    @Config(sdk = [32], application = Application::class)
    fun `pre Android 13 selection is stored in preferences`() {
        val prefs = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
        prefs.edit().putString("language_tag", "es").commit()
        assertEquals(AppLanguage.SPANISH, AppLanguageManager.selected(context))
        assertEquals("es", AppLanguageManager.resolvedLanguageTag(context))
    }

    @Test
    @Config(sdk = [32], application = Application::class)
    fun `select on Android 12 stores the language without marking it migrated`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        AppLanguageManager.select(activity, AppLanguage.SPANISH)

        val prefs = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
        assertEquals("es", prefs.getString("language_tag", ""))
        assertFalse(prefs.getBoolean("migrated_to_locale_manager", false))
        assertEquals(AppLanguage.SPANISH, AppLanguageManager.selected(context))
    }

    @Test
    @Config(sdk = [33], application = Application::class)
    fun `a language picked on Android 12 migrates into LocaleManager after an upgrade to 13`() {
        // Prefs as written by select() on API 32: the tag is stored, but the
        // migrated flag must still be unset so the upgrade path can copy it.
        val prefs = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("language_tag", "es").commit()
        assertFalse(prefs.getBoolean("migrated_to_locale_manager", false))

        assertEquals(AppLanguage.SPANISH, AppLanguageManager.selected(context))
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        assertEquals("es", locales[0].toLanguageTag())
        assertTrue(prefs.getBoolean("migrated_to_locale_manager", false))
        assertEquals("es", AppLanguageManager.resolvedLanguageTag(context))
    }

    @Test
    @Config(sdk = [33], application = Application::class)
    fun `legacy preference migrates into LocaleManager after an OS upgrade`() {
        val prefs = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("language_tag", "es").commit()

        assertEquals(AppLanguage.SPANISH, AppLanguageManager.selected(context))
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        assertEquals("es", locales[0].toLanguageTag())
        assertTrue(prefs.getBoolean("migrated_to_locale_manager", false))
        assertEquals("es", AppLanguageManager.resolvedLanguageTag(context))
    }

    @Test
    @Config(sdk = [33], application = Application::class)
    fun `migration does not overwrite a later system app-language choice`() {
        val prefs = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("language_tag", "es").commit()
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags("pt-BR")

        assertEquals(AppLanguage.PORTUGUESE_BRAZIL, AppLanguageManager.selected(context))
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        assertEquals("pt-BR", locales[0].toLanguageTag())
        assertTrue(prefs.getBoolean("migrated_to_locale_manager", false))
    }

    @Test
    @Config(sdk = [33], application = Application::class)
    fun `migration does not reapply a legacy tag after the user returns to the device language`() {
        val prefs = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
        prefs.edit()
            .clear()
            .putString("language_tag", "es")
            .putBoolean("migrated_to_locale_manager", true)
            .commit()
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.getEmptyLocaleList()

        assertEquals(AppLanguage.SYSTEM, AppLanguageManager.selected(context))
        assertTrue(context.getSystemService(LocaleManager::class.java).applicationLocales.isEmpty)
    }
}
