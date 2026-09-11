package com.wiccan.app.ui.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Muted-brass safety callout — stays in-palette (per the visual spec)
 * rather than resorting to a jarring red.
 */
@Composable
fun SafetyNote(note: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.tertiaryContainer.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .border(1.dp, colors.tertiary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // Small "◈" marker in brass — no icon-font dep needed.
        Text(
            text = "◈",
            style = MaterialTheme.typography.titleMedium,
            color = colors.tertiary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "A NOTE ON SAFETY",
                style = MaterialTheme.typography.labelSmall,
                color = colors.tertiary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onTertiaryContainer,
            )
        }
    }
}
