package com.nyxwell.app.widget

import com.nyxwell.app.domain.repository.ContentRepository
import com.nyxwell.app.domain.repository.SubscriptionRepository
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
interface NyxwellWidgetEntryPoint {
    fun contentRepository(): ContentRepository
    fun subscriptionRepository(): SubscriptionRepository
}
