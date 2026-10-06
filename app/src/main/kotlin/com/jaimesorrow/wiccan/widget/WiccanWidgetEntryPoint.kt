package com.jaimesorrow.wiccan.widget

import com.jaimesorrow.wiccan.data.local.ThemePreferences
import com.jaimesorrow.wiccan.domain.repository.ContentRepository
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * GlanceAppWidget instances are created by the Glance framework, not by
 * Hilt, so this EntryPoint is how the widget reaches the same repository
 * singletons the rest of the app uses.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WiccanWidgetEntryPoint {
    fun contentRepository(): ContentRepository
    fun subscriptionRepository(): SubscriptionRepository
    fun themePreferences(): ThemePreferences
}
