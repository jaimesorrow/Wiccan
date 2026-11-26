package com.jaimesorrow.wiccan.widget

import android.content.Context
import android.content.Intent
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
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.wiccan.app.R

/**
 * Opens the gated app. A launcher may retain a rendered widget indefinitely,
 * so lesson text must never be embedded where it could outlive a subscription.
 */
class WiccanWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val colorScheme = WiccanPaletteColors.Grimoire
        val title = context.getString(R.string.widget_title)
        val subtitle = context.getString(R.string.widget_subtitle)
        val openApp = actionStartActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(WiccanDeepLinks.TODAY_URI), context, MainActivity::class.java),
        )
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().background(ColorProvider(colorScheme.Background))
                    .padding(16.dp).clickable(openApp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = TextStyle(fontSize = 20.sp, color = ColorProvider(colorScheme.Gold)),
                )
                Text(
                    text = subtitle,
                    style = TextStyle(fontSize = 14.sp, color = ColorProvider(colorScheme.Text)),
                )
            }
        }
    }
}
