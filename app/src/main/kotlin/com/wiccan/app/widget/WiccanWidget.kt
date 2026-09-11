package com.wiccan.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.wiccan.app.MainActivity
import com.wiccan.app.domain.model.Content
import com.wiccan.app.domain.model.ContentTier
import com.wiccan.app.ui.navigation.WiccanDeepLinks
import com.wiccan.app.ui.theme.WiccanColors
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/**
 * Single-size home-screen widget teasing today's content. Gated to
 * premium subscribers per spec — free users see a locked placeholder,
 * never real content. Data is read fresh from the same repositories the
 * app uses (Room-cached content, server-synced tier) each time Glance
 * asks for a render, rather than a separate widget-owned state store.
 */
class WiccanWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WiccanWidgetEntryPoint::class.java)
        val tier = entryPoint.subscriptionRepository().observeTier().first()
        val content = entryPoint.contentRepository().observeTodayContent().first()

        provideContent {
            WidgetContent(context = context, tier = tier, content = content)
        }
    }
}

@Composable
private fun WidgetContent(context: Context, tier: ContentTier, content: Content?) {
    val deepLinkUri = if (tier != ContentTier.PREMIUM) WiccanDeepLinks.PREMIUM_URI else WiccanDeepLinks.TODAY_URI
    val clickAction = actionStartActivityIntent(
        Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri), context, MainActivity::class.java),
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WiccanColors.Background))
            .padding(16.dp)
            .clickable(clickAction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            tier != ContentTier.PREMIUM -> LockedContent()
            content != null -> PremiumTeaser(content)
            else -> EmptyContent()
        }
    }
}

@Composable
private fun LockedContent() {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🔒",
            style = TextStyle(fontSize = 24.sp, color = ColorProvider(WiccanColors.Brass)),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Upgrade to Wiccan Premium for widget access",
            style = TextStyle(fontSize = 13.sp, color = ColorProvider(WiccanColors.OnSurfaceMuted)),
        )
    }
}

@Composable
private fun PremiumTeaser(content: Content) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = content.type.name,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(WiccanColors.DeepPurple),
            ),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = content.title,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(WiccanColors.OnBackground),
            ),
            maxLines = 2,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = content.intro,
            style = TextStyle(fontSize = 12.sp, color = ColorProvider(WiccanColors.OnSurfaceMuted)),
            maxLines = 2,
        )
    }
}

@Composable
private fun EmptyContent() {
    Text(
        text = "Today's content isn't ready yet",
        style = TextStyle(fontSize = 13.sp, color = ColorProvider(WiccanColors.OnSurfaceMuted)),
    )
}
