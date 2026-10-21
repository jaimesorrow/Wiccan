package com.jaimesorrow.wiccan.ui.today.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Thin-line crescent drawn from the difference between two circular paths.
 * Drawing the outline never erases the surrounding screen background.
 */
@Composable
fun CrescentGlyph(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.tertiary,
) {
    Canvas(modifier = modifier.size(28.dp)) {
        val stroke = Stroke(width = 1.2f.dp.toPx())
        val radius = size.minDimension / 2f - stroke.width
        val centerX = size.width * 0.66f
        val centerY = size.height / 2f
        val outer = Path().apply {
            addOval(Rect(centerX - radius, centerY - radius, centerX + radius, centerY + radius))
        }
        val innerX = centerX + radius * 0.55f
        val inner = Path().apply {
            addOval(Rect(innerX - radius, centerY - radius, innerX + radius, centerY + radius))
        }
        drawPath(Path.combine(PathOperation.Difference, outer, inner), color = tint, style = stroke)
    }
}

/** Hairline horizontal rule for subtle section breaks. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}
