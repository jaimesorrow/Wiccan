package com.jaimesorrow.wiccan.ui.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Irregular torn-page silhouette for the lesson step block only. */
private class TatteredParchmentShape(
    private val seed: Int,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = buildTatteredPath(size.width, size.height, seed)
        return Outline.Generic(path)
    }
}

private fun buildTatteredPath(width: Float, height: Float, seed: Int): Path {
    val path = Path()
    if (width < 12f || height < 12f) {
        path.addRect(androidx.compose.ui.geometry.Rect(0f, 0f, width.coerceAtLeast(1f), height.coerceAtLeast(1f)))
        return path
    }
    val random = Random(seed)
    val points = 48
    val inset = 6f
    val maxX = width - inset
    val maxY = height - inset
    for (i in 0 until points) {
        val angle = (i.toFloat() / points) * 2f * PI.toFloat()
        val ripple = 0.92f + random.nextFloat() * 0.06f
        val jag = if (random.nextFloat() < 0.22f) random.nextFloat() * 0.04f else 0f
        val rx = (width * 0.48f) * (ripple + jag) * cos(angle) + width / 2f
        val ry = (height * 0.48f) * (ripple - jag * 0.6f) * sin(angle) + height / 2f
        val x = rx.coerceIn(inset, maxX.coerceAtLeast(inset))
        val y = ry.coerceIn(inset, maxY.coerceAtLeast(inset))
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Aged parchment panel drawn in Compose (no photo textures): cream fill, foxing,
 * and a scorched edge gradient. Content sits on top with real text.
 */
@Composable
fun LessonTatteredParchment(
    modifier: Modifier = Modifier,
    seed: Int = 31,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = remember(seed) { TatteredParchmentShape(seed) }
    val parchment = WiccanPaletteColors.Parchment.Background
    val inkStain = WiccanPaletteColors.Parchment.Ink.copy(alpha = 0.08f)
    val burn = WiccanPaletteColors.Parchment.Border.copy(alpha = 0.35f)
    Box(modifier.clip(shape)) {
        Canvas(Modifier.fillMaxSize()) {
            val outline = buildTatteredPath(size.width, size.height, seed)
            drawPath(outline, parchment)
            drawPath(
                outline,
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, burn),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = min(size.width, size.height) * 0.58f,
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()),
            )
            val random = Random(seed + 7)
            repeat(28) {
                val spot = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
                drawCircle(inkStain, radius = 2.dp.toPx() + random.nextFloat() * 5.dp.toPx(), spot)
            }
            repeat(6) {
                val spot = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
                drawCircle(WiccanPaletteColors.Parchment.Border.copy(alpha = 0.12f), 8.dp.toPx(), spot)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            content()
        }
    }
}
