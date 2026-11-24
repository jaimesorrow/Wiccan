package com.jaimesorrow.wiccan.ui.content

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.ui.archive.displayName
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanEmblem
import com.jaimesorrow.wiccan.ui.brand.wiccanPageWash
import com.jaimesorrow.wiccan.ui.today.components.ContentTypePill
import com.jaimesorrow.wiccan.ui.today.components.MaterialsCard
import com.jaimesorrow.wiccan.ui.today.components.SafetyNote
import com.wiccan.app.R

private val DailyParchment = listOf(Color(0xFFE4C891), Color(0xFFF1DEC0), Color(0xFFD8B77F))
private val DailyInk = Color(0xFF2F2418)
private val DailyGold = Color(0xFF9D7C3F)

/**
 * The full display for one piece of content — header (type pill,
 * tradition, category, decorative glyph), title, introduction, lesson, reflection,
 * practice, safety and source/rights sections. Shared between Today and the Grimoire detail
 * view so the two never drift into two different layouts.
 */
@Composable
fun ContentDetailView(
    content: Content,
    modifier: Modifier = Modifier,
    isDailyLesson: Boolean = false,
    onOpenGrimoire: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val ink = if (isDailyLesson) DailyInk else colors.onBackground
    val secondaryInk = if (isDailyLesson) DailyInk.copy(alpha = 0.76f) else colors.onSurfaceVariant
    val pageShape = RoundedCornerShape(18.dp)
    val pageModifier = if (isDailyLesson) {
        Modifier
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .background(Brush.verticalGradient(DailyParchment), pageShape)
            .border(1.dp, DailyGold, pageShape)
            .padding(4.dp)
            .border(1.dp, DailyGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    } else {
        Modifier.wiccanPageWash()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(pageModifier)
            .padding(PaddingValues(horizontal = 24.dp, vertical = 32.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (isDailyLesson) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                CandleOrnament(Modifier.size(width = 52.dp, height = 74.dp))
                Text(
                    text = "✦  ☾  ✦",
                    style = MaterialTheme.typography.titleMedium,
                    color = DailyGold,
                )
            }
            Text(
                stringResource(
                    when (content.type) {
                        ContentType.SPELL -> R.string.today_spell
                        ContentType.POTION -> R.string.today_potion
                        ContentType.LESSON, ContentType.UNKNOWN -> R.string.today_lesson
                    },
                ),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = ink,
            )
            Text(
                stringResource(R.string.today_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = secondaryInk,
            )
        }
        // Compact brand emblem, centered above the header row.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            WiccanEmblem(
                modifier = Modifier.testTag("content-logo"),
                size = 32.dp,
                contentDescription = stringResource(R.string.content_logo_description),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ContentTypePill(content.type)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = content.tradition,
                style = MaterialTheme.typography.titleMedium,
                color = secondaryInk,
            )
        }

        Text(
            text = content.title,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Serif,
                fontStyle = if (isDailyLesson) FontStyle.Italic else FontStyle.Normal,
            ),
            color = ink,
            textAlign = if (isDailyLesson) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )

        if (isDailyLesson) {
            DailyLessonHero(content)
        }

        Text(
            text = content.category.displayName(),
            style = MaterialTheme.typography.labelLarge,
            color = secondaryInk,
        )

        content.summary?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.titleMedium, color = secondaryInk)
        }

        content.sourceTradition?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_source_tradition), it, ink, isDailyLesson)
        }

        if (content.intro.isNotBlank()) {
            Text(
                text = content.intro,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryInk,
            )
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WiccanBotanicalAccent(width = 176.dp)
        }

        content.application?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_application), it, ink, isDailyLesson)
        }
        val displayedTools = content.tools.ifEmpty { content.materials.orEmpty() }
        if (displayedTools.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            MaterialsCard(materials = displayedTools)
        }
        LessonSection(
            title = stringResource(R.string.content_method),
            text = content.body,
            color = ink,
            parchment = isDailyLesson,
        )
        content.incantation?.takeIf { it.isNotBlank() }?.let {
            IncantationCard(text = it)
        }
        content.incantationOmissionReason?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_incantation_note), it, ink, isDailyLesson)
        }
        content.timing?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_timing), it, ink, isDailyLesson)
        }
        content.correspondences.takeIf { it.isNotEmpty() }?.let {
            LessonSection(
                stringResource(R.string.content_correspondences),
                it.joinToString(separator = "\n") { item -> "• $item" },
                ink,
                isDailyLesson,
            )
        }
        content.reflection?.takeIf { it.isNotBlank() }?.let { ReflectionSection(text = it, color = ink) }
        content.practice?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_practice), it, ink, isDailyLesson)
        }

        content.safetyNote?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(modifier = Modifier.height(8.dp))
            SafetyNote(note = note)
        }

        if (content.requiresPotionDisclosure) {
            SafetyNote(
                note = content.potionSafetyDisclosure?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.content_potion_disclosure_missing),
            )
        }

        content.adaptationNotes.takeIf { it.isNotEmpty() }?.let {
            LessonSection(
                stringResource(R.string.content_adaptation),
                it.joinToString(separator = "\n") { item -> "• $item" },
                ink,
                isDailyLesson,
            )
        }

        content.sourceCitation?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_source), it, ink, isDailyLesson)
        }
        content.sourceUrl?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = secondaryInk)
        }
        content.rightsNotes?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_rights), it, ink, isDailyLesson)
        }
        content.citation?.let { citation ->
            val details = listOfNotNull(
                citation.textTitle?.let { stringResource(R.string.content_citation_text, it) },
                citation.manuscriptId?.let { stringResource(R.string.content_citation_manuscript, it) },
                citation.tradition?.let { stringResource(R.string.content_citation_tradition, it) },
                citation.manuscriptDate?.let { stringResource(R.string.content_citation_manuscript_date, it) },
                citation.compositionDate?.let { stringResource(R.string.content_citation_composition_date, it) },
                citation.edition?.let { stringResource(R.string.content_citation_edition, it) },
                citation.editionDate?.let { stringResource(R.string.content_citation_edition_date, it) },
                citation.translator?.let { stringResource(R.string.content_citation_translator, it) },
                citation.locator?.let { stringResource(R.string.content_citation_locator, it) },
                citation.url,
            ).filter { it.isNotBlank() }
            if (details.isNotEmpty()) {
                LessonSection(
                    stringResource(R.string.content_verified_source),
                    details.joinToString("\n"),
                    ink,
                    isDailyLesson,
                )
            }
        }

        if (isDailyLesson) {
            Text(
                stringResource(R.string.content_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryInk,
            )
            onOpenGrimoire?.let { action ->
                TextButton(onClick = action) { Text(stringResource(R.string.today_open_grimoire)) }
            }
        }
        if (isDailyLesson) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                QuillAndInkpotOrnament(Modifier.size(width = 112.dp, height = 88.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LessonSection(
    title: String,
    text: String,
    color: Color? = null,
    parchment: Boolean = false,
) {
    val ink = color ?: MaterialTheme.colorScheme.onBackground
    val family = if (parchment) FontFamily.Serif else FontFamily.Default
    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif), color = ink)
    Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = family), color = ink)
}

@Composable
private fun DailyLessonHero(content: Content) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(shape)
            .border(1.dp, DailyGold.copy(alpha = 0.72f), shape),
    ) {
        Image(
            painter = painterResource(content.lessonArtwork()),
            contentDescription = stringResource(R.string.content_lesson_image_description, content.title),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to DailyParchment[1],
                    ),
                ),
        )
    }
}

@DrawableRes
private fun Content.lessonArtwork(): Int {
    when (illustrationKey?.trim()?.lowercase()) {
        "lunar", "moon", "moon-phases" -> return R.drawable.wiccan_category_lunar
        "cord-knot", "cord", "knot-work" -> return R.drawable.wiccan_category_cord
        "grimoire", "ritual", "botanical", "herbal", "candle" -> return R.drawable.wiccan_home_hero
    }
    val categoryArtwork = when (category) {
        ContentCategory.CORD_KNOT_WORK, ContentCategory.CORD_KNOT_MAGIC -> R.drawable.wiccan_category_cord
        ContentCategory.LUNAR_PRACTICES,
        ContentCategory.MOON_MAGIC_TIMING,
        ContentCategory.MOON_AND_SEASONAL -> R.drawable.wiccan_category_lunar
        else -> R.drawable.wiccan_home_hero
    }
    // Content identity adds deterministic variation for lessons in broad categories while
    // preserving a stable illustration for the same delivered lesson across recompositions.
    return when (Math.floorMod(id.hashCode(), 5)) {
        0 -> R.drawable.wiccan_category_lunar
        1 -> R.drawable.wiccan_category_cord
        else -> categoryArtwork
    }
}

@Composable
private fun CandleOrnament(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(
            DailyGold,
            Offset(size.width * .35f, size.height * .42f),
            Offset(size.width * .35f, size.height * .92f),
            stroke.width,
        )
        drawLine(
            DailyGold,
            Offset(size.width * .65f, size.height * .42f),
            Offset(size.width * .65f, size.height * .92f),
            stroke.width,
        )
        drawLine(
            DailyGold,
            Offset(size.width * .35f, size.height * .92f),
            Offset(size.width * .65f, size.height * .92f),
            stroke.width,
        )
        val flame = Path().apply {
            moveTo(size.width * .5f, size.height * .04f)
            cubicTo(
                size.width * .76f,
                size.height * .19f,
                size.width * .62f,
                size.height * .37f,
                size.width * .5f,
                size.height * .4f,
            )
            cubicTo(
                size.width * .34f,
                size.height * .34f,
                size.width * .31f,
                size.height * .19f,
                size.width * .5f,
                size.height * .04f,
            )
            close()
        }
        drawPath(flame, Brush.verticalGradient(listOf(Color(0xFFE5C158), Color(0xFFA67C00))))
    }
}

@Composable
private fun QuillAndInkpotOrnament(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val line = 1.4.dp.toPx()
        val quill = Path().apply {
            moveTo(size.width * .14f, size.height * .82f)
            cubicTo(
                size.width * .27f,
                size.height * .5f,
                size.width * .55f,
                size.height * .12f,
                size.width * .85f,
                size.height * .08f,
            )
            cubicTo(
                size.width * .74f,
                size.height * .39f,
                size.width * .43f,
                size.height * .58f,
                size.width * .14f,
                size.height * .82f,
            )
        }
        drawPath(quill, DailyInk, style = Stroke(width = line, cap = StrokeCap.Round))
        drawLine(
            DailyInk,
            Offset(size.width * .14f, size.height * .82f),
            Offset(size.width * .8f, size.height * .13f),
            line,
        )
        drawLine(
            DailyInk,
            Offset(size.width * .38f, size.height * .59f),
            Offset(size.width * .31f, size.height * .44f),
            line,
        )
        drawLine(
            DailyInk,
            Offset(size.width * .5f, size.height * .46f),
            Offset(size.width * .42f, size.height * .31f),
            line,
        )
        drawArc(
            color = DailyInk,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * .62f, size.height * .65f),
            size = Size(size.width * .3f, size.height * .2f),
            style = Stroke(line),
        )
        drawRect(
            color = DailyInk,
            topLeft = Offset(size.width * .64f, size.height * .73f),
            size = Size(size.width * .26f, size.height * .18f),
            style = Stroke(line),
        )
    }
}
