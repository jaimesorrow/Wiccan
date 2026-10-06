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
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.ui.archive.displayName
import com.jaimesorrow.wiccan.ui.today.components.ContentTypePill
import com.jaimesorrow.wiccan.ui.today.components.CrescentGlyph
import com.jaimesorrow.wiccan.ui.today.components.HairlineDivider
import com.jaimesorrow.wiccan.ui.today.components.MaterialsCard
import com.jaimesorrow.wiccan.ui.today.components.SafetyNote

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
                "${content.type.name.lowercase().replaceFirstChar { it.uppercase() }} of the Day",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onBackground,
            )
        }
        // Decorative crescent, centered above the header row.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CrescentGlyph()
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

        if (content.intro.isNotBlank()) {
            Text(
                text = content.intro,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }

        HairlineDivider(modifier = Modifier.padding(vertical = 8.dp))

        LessonSection(title = "Lesson", text = content.body)
        content.reflection?.takeIf { it.isNotBlank() }?.let { LessonSection("Reflection", it) }
        content.practice?.takeIf { it.isNotBlank() }?.let { LessonSection("Practice", it) }

        content.materials?.takeIf { it.isNotEmpty() }?.let { materials ->
            Spacer(modifier = Modifier.height(8.dp))
            MaterialsCard(materials = materials)
        }

        content.safetyNote?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(modifier = Modifier.height(8.dp))
            SafetyNote(note = note)
        }

        content.sourceCitation?.takeIf { it.isNotBlank() }?.let { LessonSection("Source", it) }
        content.sourceUrl?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        content.rightsNotes?.takeIf { it.isNotBlank() }?.let { LessonSection("Rights", it) }

        if (isDailyLesson) {
            Text("Saved to Grimoire", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            onOpenGrimoire?.let { action ->
                TextButton(onClick = action) { Text("Open Grimoire") }
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
