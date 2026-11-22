package com.jaimesorrow.wiccan.ui.content

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.ui.archive.displayName
import com.jaimesorrow.wiccan.ui.brand.WiccanEmblem
import com.jaimesorrow.wiccan.ui.today.components.ContentTypePill
import com.jaimesorrow.wiccan.ui.today.components.HairlineDivider
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
    isDailyLesson: Boolean = false,
    onOpenGrimoire: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 24.dp, vertical = 32.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (isDailyLesson) {
            Text(
                stringResource(
                    when (content.type) {
                        ContentType.SPELL -> R.string.today_spell
                        ContentType.POTION -> R.string.today_potion
                        ContentType.LESSON, ContentType.UNKNOWN -> R.string.today_lesson
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onBackground,
            )
            Text(
                stringResource(R.string.today_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
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
                color = colors.onSurfaceVariant,
            )
        }

        Text(
            text = content.title,
            style = MaterialTheme.typography.displayLarge,
            color = colors.onBackground,
        )

        Text(
            text = content.category.displayName(),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )

        content.summary?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
        }

        content.sourceTradition?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_source_tradition), it)
        }

        if (content.intro.isNotBlank()) {
            Text(
                text = content.intro,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }

        HairlineDivider(modifier = Modifier.padding(vertical = 8.dp))

        content.application?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_application), it)
        }
        val displayedTools = content.tools.ifEmpty { content.materials.orEmpty() }
        if (displayedTools.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            MaterialsCard(materials = displayedTools)
        }
        LessonSection(title = stringResource(R.string.content_method), text = content.body)
        content.incantation?.takeIf { it.isNotBlank() }?.let {
            IncantationCard(text = it)
        }
        content.incantationOmissionReason?.takeIf { it.isNotBlank() }?.let {
            LessonSection(stringResource(R.string.content_incantation_note), it)
        }
        content.timing?.takeIf { it.isNotBlank() }?.let { LessonSection(stringResource(R.string.content_timing), it) }
        content.correspondences.takeIf { it.isNotEmpty() }?.let {
            LessonSection(
                stringResource(R.string.content_correspondences),
                it.joinToString(separator = "\n") { item -> "• $item" },
            )
        }
        content.reflection?.takeIf { it.isNotBlank() }?.let { ReflectionSection(text = it) }
        content.practice?.takeIf { it.isNotBlank() }?.let { LessonSection(stringResource(R.string.content_practice), it) }

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
            )
        }

        content.sourceCitation?.takeIf { it.isNotBlank() }?.let { LessonSection(stringResource(R.string.content_source), it) }
        content.sourceUrl?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        content.rightsNotes?.takeIf { it.isNotBlank() }?.let { LessonSection(stringResource(R.string.content_rights), it) }
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
                LessonSection(stringResource(R.string.content_verified_source), details.joinToString("\n"))
            }
        }

        if (isDailyLesson) {
            Text(
                stringResource(R.string.content_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            onOpenGrimoire?.let { action ->
                TextButton(onClick = action) { Text(stringResource(R.string.today_open_grimoire)) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LessonSection(title: String, text: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
}
