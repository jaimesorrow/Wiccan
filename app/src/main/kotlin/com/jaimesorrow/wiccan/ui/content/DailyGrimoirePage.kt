package com.jaimesorrow.wiccan.ui.content

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.sky.CelestialCalculator
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.wiccan.app.R
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Quill = FontFamily(Font(R.font.italianno_regular))
private val Ink = WiccanPaletteColors.Parchment.Ink
private val Midnight = WiccanPaletteColors.Grimoire.Background
private val Sepia = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Serif, color = Ink)
private val Betelgeuse = Color(0xFFE36F47)

internal fun seasonalChipText(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): String? {
    val sabbat = mapOf(
        MonthDay.of(2, 1) to "Imbolc",
        MonthDay.of(3, 20) to "Ostara",
        MonthDay.of(5, 1) to "Beltane",
        MonthDay.of(6, 21) to "Litha",
        MonthDay.of(8, 1) to "Lughnasadh",
        MonthDay.of(9, 22) to "Mabon",
        MonthDay.of(10, 31) to "Samhain",
        MonthDay.of(12, 21) to "Yule",
    )[MonthDay.from(date)]
    if (sabbat != null) return sabbat
    val instant = date.atTime(12, 0).atZone(zoneId).toInstant()
    val moon = CelestialCalculator.moon(instant)
    return moon.name.takeIf { it == "New Moon" || it == "Full Moon" }
}

@Composable
internal fun DailyGrimoirePage(
    content: Content,
    modifier: Modifier = Modifier,
    onOpenGrimoire: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Midnight),
    ) {
        LessonHero(content)
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
                .border(1.dp, WiccanPaletteColors.Parchment.Border, RoundedCornerShape(4.dp))
                .background(WiccanPaletteColors.Parchment.Background)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            seasonalChipText(LocalDate.now())?.let { event ->
                Text(
                    text = if (event.endsWith("Moon")) stringResource(R.string.today_seasonal_moon, event) else event,
                    color = WiccanPaletteColors.Grimoire.OnGold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Betelgeuse, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            CandleOrnament(Modifier.size(width = 36.dp, height = 52.dp).align(Alignment.Start))
            Text(
                text = content.title,
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = Quill, fontSize = 42.sp, lineHeight = 46.sp),
            )
            Text(
                text = stringResource(R.string.today_lesson),
                color = Ink,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.today_subtitle),
                color = Ink.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
            )
            val tools = content.tools.ifEmpty { content.materials.orEmpty() }
            var step = 1
            if (tools.isNotEmpty()) {
                SectionTitle(stringResource(R.string.content_materials))
                tools.forEach { item ->
                    Text("☐  $item", color = Ink, style = MaterialTheme.typography.bodyLarge.merge(Sepia))
                }
                WiccanBotanicalAccent(width = 96.dp)
            }
            if (content.intro.isNotBlank()) {
                Text(content.intro, color = Ink, style = MaterialTheme.typography.bodyLarge.merge(Sepia))
            }
            SectionTitle("${roman(step++)}. ${stringResource(R.string.content_method)}")
            Text(content.body, color = Ink, style = MaterialTheme.typography.bodyLarge.merge(Sepia))
            WiccanBotanicalAccent(width = 96.dp)
            content.reflection?.takeIf { it.isNotBlank() }?.let {
                SectionTitle("${roman(step++)}. ${stringResource(R.string.content_reflection)}")
                Text(it, color = Ink, style = MaterialTheme.typography.bodyLarge.merge(Sepia))
            }
            content.practice?.takeIf { it.isNotBlank() }?.let {
                SectionTitle("${roman(step++)}. ${stringResource(R.string.content_practice)}")
                Text(it, color = Ink, style = MaterialTheme.typography.bodyLarge.merge(Sepia))
            }
            content.safetyNote?.takeIf { it.isNotBlank() }?.let {
                SectionTitle(stringResource(R.string.content_safety_heading))
                Text(it, color = Ink, style = MaterialTheme.typography.bodyMedium.merge(Sepia))
            }
            content.sourceCitation?.takeIf { it.isNotBlank() }?.let {
                SectionTitle(stringResource(R.string.content_source))
                Text(it, color = Ink, style = MaterialTheme.typography.bodyMedium)
            }
            content.rightsNotes?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Ink, style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.content_saved), color = Ink, style = MaterialTheme.typography.bodyMedium)
            onOpenGrimoire?.let { action ->
                TextButton(onClick = action) {
                    Text(stringResource(R.string.today_open_grimoire), color = WiccanPaletteColors.Grimoire.Bronze)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                QuillAndInkpotOrnament(Modifier.size(width = 96.dp, height = 72.dp))
            }
        }
    }
}

private fun roman(value: Int): String {
    val numerals = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
    return numerals.getOrElse(value - 1) { value.toString() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = WiccanPaletteColors.Grimoire.Bronze,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun LessonHero(content: Content) {
    Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
        RemoteOrBundledImage(
            url = content.illustrationUrl,
            fallback = content.categoryArtwork(),
            description = stringResource(R.string.content_lesson_image_description, content.title),
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Midnight.copy(alpha = 0.05f), Midnight)),
            ),
        )
    }
}

@Composable
private fun RemoteOrBundledImage(
    url: String?,
    @DrawableRes fallback: Int,
    description: String,
    modifier: Modifier = Modifier,
) {
    var failed by remember(url) { mutableStateOf(url.isNullOrBlank() || !url.startsWith("https://")) }
    var remote by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        if (url.isNullOrBlank() || !url.startsWith("https://")) {
            failed = true
            return@LaunchedEffect
        }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    instanceFollowRedirects = true
                }
                connection.inputStream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
        if (bitmap == null) failed = true else remote = bitmap
    }
    val image = remote
    if (!failed && image != null) {
        Image(bitmap = image, contentDescription = description, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Image(
            painter = painterResource(fallback),
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@DrawableRes
private fun Content.categoryArtwork(): Int = when (category) {
    ContentCategory.LUNAR_PRACTICES,
    ContentCategory.MOON_MAGIC_TIMING,
    ContentCategory.MOON_AND_SEASONAL,
    -> R.drawable.wiccan_photo_moon
    ContentCategory.HERBAL_RITUALS, ContentCategory.HERBS_BOTANICALS -> R.drawable.wiccan_photo_herbs
    ContentCategory.POTIONS_INFUSIONS -> R.drawable.wiccan_photo_cauldron
    ContentCategory.CRYSTALS_STONES -> R.drawable.wiccan_photo_crystals
    ContentCategory.DIVINATION, ContentCategory.DIVINATION_AND_CLARITY -> R.drawable.wiccan_photo_divination
    ContentCategory.ALTARS_SACRED_SPACE -> R.drawable.wiccan_photo_wheel
    ContentCategory.CANDLE_SPELLS, ContentCategory.CANDLE_MAGIC -> R.drawable.wiccan_photo_zodiac
    ContentCategory.HOME_AND_HEARTH -> R.drawable.wiccan_photo_journal
    else -> R.drawable.wiccan_photo_handwritten
}
