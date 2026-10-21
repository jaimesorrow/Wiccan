package com.jaimesorrow.wiccan.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jaimesorrow.wiccan.MainActivity
import com.jaimesorrow.wiccan.ui.navigation.WiccanDeepLinks
import com.jaimesorrow.wiccan.ui.theme.resolveWiccanPalette
import com.jaimesorrow.wiccan.ui.theme.toColorScheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/**
 * Opens the gated app. A launcher may retain a rendered widget indefinitely,
 * so lesson text must never be embedded where it could outlive a subscription.
 */
class WiccanWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WiccanWidgetEntryPoint::class.java)
        val themeMode = entryPoint.themePreferences().themeMode.first()
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val colorScheme = resolveWiccanPalette(themeMode, isSystemDark).toColorScheme()
        val openApp = actionStartActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(WiccanDeepLinks.TODAY_URI), context, MainActivity::class.java),
        )
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().background(ColorProvider(colorScheme.background))
                    .padding(16.dp).clickable(openApp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Wiccan",
                    style = TextStyle(fontSize = 20.sp, color = ColorProvider(colorScheme.primary)),
                )
                Text(
                    text = "Open your daily practice",
                    style = TextStyle(fontSize = 14.sp, color = ColorProvider(colorScheme.onBackground)),
                )
            }
        }
    }
}
