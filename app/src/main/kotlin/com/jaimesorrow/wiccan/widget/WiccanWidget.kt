package com.jaimesorrow.wiccan.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jaimesorrow.wiccan.MainActivity
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.ui.navigation.WiccanDeepLinks
import com.jaimesorrow.wiccan.ui.theme.resolveWiccanPalette
import com.jaimesorrow.wiccan.ui.theme.toColorScheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/**
 * Single-size home-screen widget teasing today's content. Gated to
 * premium subscribers per spec — free users see a locked placeholder,
 * never real content. Data is read fresh from the same repositories the
 * app uses (Room-cached content, server-synced tier) each time Glance
 * asks for a render, rather than a separate widget-owned state store.
 *
 * Glance renders in its own composition, separate from the in-app Compose
 * tree, so it can't read `MaterialTheme.colorScheme` - instead it resolves
 * the same active [com.jaimesorrow.wiccan.ui.theme.WiccanPalette] directly
 * (discrete, not the in-app cross-fade animation, since Glance re-renders
 * on its own schedule rather than continuously) and pulls colors from the
 * same centralized [ColorScheme] every other screen uses.
 */
class WiccanWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WiccanWidgetEntryPoint::class.java)
        val tier = entryPoint.subscriptionRepository().observeTier().first()
        val content = entryPoint.contentRepository().observeTodayContent().first()
        val themeMode = entryPoint.themePreferences().themeMode.first()
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val colorScheme = resolveWiccanPalette(themeMode, isSystemDark).toColorScheme()

        provideContent {
            WidgetContent(context = context, tier = tier, content = content, colorScheme = colorScheme)
        }
    }
}

@Composable
private fun WidgetContent(context: Context, tier: ContentTier, content: Content?, colorScheme: ColorScheme) {
    val deepLinkUri = if (tier != ContentTier.PREMIUM) WiccanDeepLinks.PREMIUM_URI else WiccanDeepLinks.TODAY_URI
    val clickAction = actionStartActivityIntent(
        Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri), context, MainActivity::class.java),
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(colorScheme.background))
            .padding(16.dp)
            .clickable(clickAction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            tier != ContentTier.PREMIUM -> LockedContent(colorScheme)
            content != null -> PremiumTeaser(content, colorScheme)
            else -> EmptyContent(colorScheme)
        }
    }
}

@Composable
private fun LockedContent(colorScheme: ColorScheme) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🔒",
            style = TextStyle(fontSize = 24.sp, color = ColorProvider(colorScheme.tertiary)),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Upgrade to Wiccan Premium for widget access",
            style = TextStyle(fontSize = 13.sp, color = ColorProvider(colorScheme.onSurfaceVariant)),
        )
    }
}

@Composable
private fun PremiumTeaser(content: Content, colorScheme: ColorScheme) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = content.type.name,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(colorScheme.primary),
            ),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = content.title,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(colorScheme.onBackground),
            ),
            maxLines = 2,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = content.intro,
            style = TextStyle(fontSize = 12.sp, color = ColorProvider(colorScheme.onSurfaceVariant)),
            maxLines = 2,
        )
    }
}

@Composable
private fun EmptyContent(colorScheme: ColorScheme) {
    Text(
        text = "Today's content is unavailable. Open Wiccan to refresh or revisit Grimoire.",
        style = TextStyle(fontSize = 13.sp, color = ColorProvider(colorScheme.onSurfaceVariant)),
    )
}
