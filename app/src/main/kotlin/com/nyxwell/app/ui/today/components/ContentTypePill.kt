package com.nyxwell.app.ui.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nyxwell.app.domain.model.ContentType

/**
 * Small pill badge showing the content type. Uses the primary container
 * tone so it reads as an accent without competing with the title.
 */
@Composable
fun ContentTypePill(type: ContentType, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val label = when (type) {
        ContentType.SPELL -> "SPELL"
        ContentType.POTION -> "POTION"
        ContentType.LESSON -> "LESSON"
    }
    Box(
        modifier = modifier
            .background(colors.primaryContainer, RoundedCornerShape(50))
            .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(PaddingValues(horizontal = 12.dp, vertical = 5.dp)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onPrimaryContainer,
        )
    }
}
