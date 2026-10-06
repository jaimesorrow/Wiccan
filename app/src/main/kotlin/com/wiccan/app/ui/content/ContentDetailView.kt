package com.wiccan.app.ui.content

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wiccan.app.R
import com.wiccan.app.domain.model.Content
import com.wiccan.app.domain.model.ContentCategory
import com.wiccan.app.domain.model.ContentDifficulty
import com.wiccan.app.domain.model.ContentType
import com.wiccan.app.ui.today.components.ContentTypePill
import com.wiccan.app.ui.today.components.CrescentGlyph
import com.wiccan.app.ui.today.components.HairlineDivider
import com.wiccan.app.ui.today.components.MaterialsCard
import com.wiccan.app.ui.today.components.SafetyNote

/**
 * The full display for one piece of content — header (type pill,
 * tradition, decorative glyph), title, intro, body, reflection and optional
 * practice/materials/safety sections. Shared between Today and the Archive detail
 * view so the two never drift into two different layouts.
 */
@Composable
fun ContentDetailView(content: Content, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 24.dp, vertical = 32.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Decorative crescent, centered above the header row.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CrescentGlyph()
        }

        Text(
            text = stringResource(content.category.labelResource()),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        content.difficulty?.let { difficulty ->
            Text(
                text = stringResource(difficulty.labelResource()),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
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

        if (content.intro.isNotBlank()) {
            if (content.type == ContentType.LESSON) {
                ContentSectionHeading(stringResource(R.string.content_introduction))
            }
            Text(
                text = content.intro,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }

        HairlineDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (content.type == ContentType.LESSON) {
            ContentSectionHeading(stringResource(R.string.content_lesson))
        }
        Text(
            text = content.body,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onBackground,
        )

        content.reflection?.takeIf { it.isNotBlank() }?.let { reflection ->
            ContentSectionHeading(stringResource(R.string.content_reflection))
            Text(
                text = reflection,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground,
            )
        }

        content.practice?.takeIf { it.isNotBlank() }?.let { practice ->
            ContentSectionHeading(stringResource(R.string.content_practice))
            Text(
                text = practice,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground,
            )
        }

        content.materials?.takeIf { it.isNotEmpty() }?.let { materials ->
            Spacer(modifier = Modifier.height(8.dp))
            MaterialsCard(materials = materials)
        }

        content.safetyNote?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(modifier = Modifier.height(8.dp))
            SafetyNote(note = note)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ContentSectionHeading(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

private fun ContentDifficulty.labelResource(): Int = when (this) {
    ContentDifficulty.BEGINNER -> R.string.content_difficulty_beginner
    ContentDifficulty.INTERMEDIATE -> R.string.content_difficulty_intermediate
    ContentDifficulty.ADVANCED -> R.string.content_difficulty_advanced
}

private fun ContentCategory.labelResource(): Int = when (this) {
    ContentCategory.PROTECTION -> R.string.content_category_protection
    ContentCategory.LOVE_AND_RELATIONSHIPS -> R.string.content_category_relationships
    ContentCategory.PROSPERITY_AND_ABUNDANCE -> R.string.content_category_prosperity
    ContentCategory.HEALING_AND_WELLNESS -> R.string.content_category_wellness
    ContentCategory.DIVINATION_AND_CLARITY -> R.string.content_category_divination
    ContentCategory.BANISHING_AND_RELEASE -> R.string.content_category_release
    ContentCategory.HOME_AND_HEARTH -> R.string.content_category_home
    ContentCategory.MOON_AND_SEASONAL -> R.string.content_category_seasonal
}
