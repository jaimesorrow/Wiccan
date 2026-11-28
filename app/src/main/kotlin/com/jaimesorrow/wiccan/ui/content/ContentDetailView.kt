package com.jaimesorrow.wiccan.ui.content

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.ui.archive.displayName
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanEmblem
import com.jaimesorrow.wiccan.ui.brand.WiccanLineArtKind
import com.jaimesorrow.wiccan.ui.brand.wiccanPageWash
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.jaimesorrow.wiccan.ui.today.components.ContentTypePill
import com.jaimesorrow.wiccan.ui.today.components.MaterialsCard
import com.jaimesorrow.wiccan.ui.today.components.SafetyNote
import com.wiccan.app.R

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
    onOpenGrimoire: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val ink = colors.onBackground
    val secondaryInk = colors.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .wiccanPageWash()
            .padding(PaddingValues(horizontal = 24.dp, vertical = 32.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
                fontStyle = FontStyle.Normal,
            ),
            color = ink,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = content.category.displayName(),
            style = MaterialTheme.typography.labelLarge,
            color = secondaryInk,
        )

        content.summary?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.titleMedium, color = secondaryInk)
        }

        content.sourceTradition?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_source_tradition), it, ink)
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
            LessonSection(stringResource(R.string.content_application), it, ink)
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
        )
        content.incantation?.takeIf { it.isNotBlank() }?.let {
            IncantationCard(text = it)
        }
        content.incantationOmissionReason?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_incantation_note), it, ink)
        }
        content.timing?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_timing), it, ink)
        }
        content.correspondences.takeIf { it.isNotEmpty() }?.let {
            LessonSection(
                stringResource(R.string.content_correspondences),
                it.joinToString(separator = "\n") { item -> "• $item" },
                ink,
            )
        }
        content.reflection?.takeIf { it.isNotBlank() }?.let { ReflectionSection(text = it, color = ink) }
        content.practice?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_practice), it, ink)
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
            )
        }

        content.sourceCitation?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_source), it, ink)
        }
        content.sourceUrl?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = secondaryInk)
        }
        content.rightsNotes?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_rights), it, ink)
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
                )
            }
        }

        onOpenGrimoire?.let { action ->
            Text(
                stringResource(R.string.content_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryInk,
            )
            TextButton(onClick = action) { Text(stringResource(R.string.today_open_grimoire)) }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LessonSection(
    title: String,
    text: String,
    color: Color? = null,
) {
    val ink = color ?: MaterialTheme.colorScheme.onBackground
    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif), color = ink)
    Text(text, style = MaterialTheme.typography.bodyLarge, color = ink)
}

@Composable
internal fun CandleOrnament(modifier: Modifier = Modifier) {
    com.jaimesorrow.wiccan.ui.brand.WiccanLineArt(
        kind = WiccanLineArtKind.Candle,
        contentDescription = stringResource(R.string.lesson_candle_description),
        modifier = modifier,
    )
}

@Composable
internal fun QuillAndInkpotOrnament(
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.lesson_quill_description),
) {
    val ink = WiccanPaletteColors.Parchment.Ink
    val brass = WiccanPaletteColors.Grimoire.Gold
    Canvas(
        modifier.semantics { this.contentDescription = contentDescription },
    ) {
        val shaft = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        drawOval(
            brass,
            topLeft = Offset(size.width * .02f, size.height * .70f),
            size = Size(size.width * .36f, size.height * .14f),
        )
        drawRect(
            brass,
            topLeft = Offset(size.width * .06f, size.height * .76f),
            size = Size(size.width * .28f, size.height * .18f),
        )
        drawOval(
            ink,
            topLeft = Offset(size.width * .10f, size.height * .72f),
            size = Size(size.width * .18f, size.height * .07f),
        )
        val feather = Path().apply {
            moveTo(size.width * .28f, size.height * .70f)
            cubicTo(
                size.width * .42f,
                size.height * .40f,
                size.width * .64f,
                size.height * .16f,
                size.width * .94f,
                size.height * .04f,
            )
            cubicTo(
                size.width * .72f,
                size.height * .22f,
                size.width * .50f,
                size.height * .40f,
                size.width * .30f,
                size.height * .66f,
            )
            close()
        }
        drawPath(feather, ink)
        drawLine(
            brass.copy(alpha = 0.55f),
            Offset(size.width * .32f, size.height * .62f),
            Offset(size.width * .82f, size.height * .12f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            ink,
            Offset(size.width * .20f, size.height * .74f),
            Offset(size.width * .86f, size.height * .10f),
            strokeWidth = shaft.width,
            cap = StrokeCap.Round,
        )
        listOf(0.46f to 0.46f, 0.58f to 0.34f, 0.70f to 0.22f).forEach { (x, y) ->
            drawLine(
                ink.copy(alpha = 0.75f),
                Offset(size.width * x, size.height * y),
                Offset(size.width * (x + 0.07f), size.height * (y - 0.08f)),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
