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
import androidx.compose.ui.unit.dp
import com.wiccan.app.domain.model.Content
import com.wiccan.app.ui.today.components.ContentTypePill
import com.wiccan.app.ui.today.components.CrescentGlyph
import com.wiccan.app.ui.today.components.HairlineDivider
import com.wiccan.app.ui.today.components.MaterialsCard
import com.wiccan.app.ui.today.components.SafetyNote

/**
 * The full display for one piece of content — header (type pill,
 * tradition, decorative glyph), title, intro, body, and the optional
 * materials/safety sections. Shared between Today and the Archive detail
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
            Text(
                text = content.intro,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }

        HairlineDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = content.body,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onBackground,
        )

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
