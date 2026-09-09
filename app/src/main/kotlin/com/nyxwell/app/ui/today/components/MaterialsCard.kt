package com.nyxwell.app.ui.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Bulleted materials list in a subtly bordered card. Uses [surfaceVariant]
 * so it lifts off the background without shouting.
 */
@Composable
fun MaterialsCard(materials: List<String>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "MATERIALS",
            style = MaterialTheme.typography.labelSmall,
            color = colors.tertiary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        materials.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(colors.tertiary, RoundedCornerShape(50)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )
            }
        }
    }
}
