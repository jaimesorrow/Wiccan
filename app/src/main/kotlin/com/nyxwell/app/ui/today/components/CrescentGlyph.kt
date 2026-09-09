package com.nyxwell.app.ui.today.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Thin-line crescent moon drawn as two overlapping circles — the second
 * uses [BlendMode.Clear] to carve the crescent out of the first. Matches
 * the spec's "subtle thin-line decorative glyph" note without pulling in
 * an asset pipeline for a single icon.
 */
@Composable
fun CrescentGlyph(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.tertiary,
) {
    Canvas(modifier = modifier.size(28.dp)) {
        val radius = size.minDimension / 2f
        val stroke = Stroke(width = 1.2f.dp.toPx())
        drawCircle(
            color = tint,
            radius = radius - stroke.width,
            center = Offset(size.width / 2f, size.height / 2f),
            style = stroke,
        )
        drawCircle(
            color = Color.Transparent,
            radius = radius - stroke.width,
            center = Offset(size.width / 2f + radius * 0.55f, size.height / 2f),
            style = Stroke(width = radius * 1.6f),
            blendMode = BlendMode.Clear,
        )
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
