package com.jaimesorrow.wiccan.ui.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import kotlin.math.max
import kotlin.random.Random

/**
 * Rectangular aged parchment for lesson steps: cream fill and foxing always sit behind
 * copy; the torn edge is a decorative stroke only (content is never clipped).
 */
@Composable
fun LessonTatteredParchment(
    modifier: Modifier = Modifier,
    seed: Int = 31,
    content: @Composable ColumnScope.() -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    val horizontalInset = (22.dp * max(fontScale, 1f)).coerceAtMost(32.dp)
    val verticalInset = (18.dp * max(fontScale, 1f)).coerceAtMost(28.dp)
    val parchment = WiccanPaletteColors.Parchment.Background
    val inkStain = WiccanPaletteColors.Parchment.Ink.copy(alpha = 0.07f)
    val burn = WiccanPaletteColors.Parchment.Border.copy(alpha = 0.42f)
    val foxing = remember(seed) {
        val r = Random(seed + 7)
        List(32) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) }
    }
    val scorchSpots = remember(seed) {
        val r = Random(seed + 11)
        List(5) { Pair(r.nextFloat(), r.nextFloat()) }
    }
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val inset = 2.dp.toPx()
                val rect = Rect(inset, inset, size.width - inset, size.height - inset)
                drawRect(parchment, topLeft = rect.topLeft, size = rect.size)
                foxing.forEach { (nx, ny, nr) ->
                    drawCircle(
                        inkStain,
                        radius = 1.5.dp.toPx() + nr * 4.dp.toPx(),
                        center = Offset(rect.left + nx * rect.width, rect.top + ny * rect.height),
                    )
                }
                scorchSpots.forEach { (nx, ny) ->
                    drawCircle(
                        WiccanPaletteColors.Parchment.Border.copy(alpha = 0.14f),
                        6.dp.toPx(),
                        Offset(rect.left + nx * rect.width, rect.top + ny * rect.height),
                    )
                }
                val edge = wavyFramePath(rect, seed, wave = 5.dp.toPx())
                drawPath(edge, burn, style = Stroke(width = 1.4.dp.toPx()))
                val scorch = wavyFramePath(rect.inflate(-3.dp.toPx()), seed + 3, wave = 3.dp.toPx())
                drawPath(
                    scorch,
                    WiccanPaletteColors.Parchment.Ink.copy(alpha = 0.12f),
                    style = Stroke(width = 0.8.dp.toPx()),
                )
            }
            .padding(horizontal = horizontalInset, vertical = verticalInset),
    ) {
        Column(Modifier.fillMaxWidth()) {
            content()
        }
    }
}

private fun wavyFramePath(rect: Rect, seed: Int, wave: Float): Path {
    val random = Random(seed)
    val path = Path()
    val steps = 18
    fun alongTop(i: Int): Offset {
        val t = i / steps.toFloat()
        val jitter = (random.nextFloat() - 0.5f) * wave
        return Offset(rect.left + rect.width * t, rect.top + jitter)
    }
    fun alongRight(i: Int): Offset {
        val t = i / steps.toFloat()
        val jitter = (random.nextFloat() - 0.5f) * wave
        return Offset(rect.right + jitter, rect.top + rect.height * t)
    }
    fun alongBottom(i: Int): Offset {
        val t = i / steps.toFloat()
        val jitter = (random.nextFloat() - 0.5f) * wave
        return Offset(rect.right - rect.width * t, rect.bottom + jitter)
    }
    fun alongLeft(i: Int): Offset {
        val t = i / steps.toFloat()
        val jitter = (random.nextFloat() - 0.5f) * wave
        return Offset(rect.left + jitter, rect.bottom - rect.height * t)
    }
    path.moveTo(alongTop(0).x, alongTop(0).y)
    for (i in 1..steps) path.lineTo(alongTop(i).x, alongTop(i).y)
    for (i in 0..steps) path.lineTo(alongRight(i).x, alongRight(i).y)
    for (i in 0..steps) path.lineTo(alongBottom(i).x, alongBottom(i).y)
    for (i in 0..steps) path.lineTo(alongLeft(i).x, alongLeft(i).y)
    path.close()
    return path
}

private fun Rect.inflate(delta: Float): Rect = Rect(
    left = left + delta,
    top = top + delta,
    right = right - delta,
    bottom = bottom - delta,
)
