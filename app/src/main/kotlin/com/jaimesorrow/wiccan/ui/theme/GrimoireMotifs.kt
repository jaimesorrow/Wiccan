package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiccan.app.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * Spell-map ornaments for the Midnight Grimoire theme: a dashed ink path,
 * a circled Caveat label, a moon-phase row, and a candlelight wash.
 */

private fun inkDash(width: Float) = Stroke(
    width = width,
    cap = StrokeCap.Round,
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(width * 5f, width * 4f), 0f),
)

@Composable
fun GrimoireCandleGlow(modifier: Modifier = Modifier) {
    val gold = WiccanPaletteColors.Grimoire.Gold
    Canvas(modifier) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(gold.copy(alpha = 0.42f), gold.copy(alpha = 0f)),
                center = Offset(size.width * 0.55f, size.height * 0.42f),
                radius = size.minDimension * 0.58f,
            ),
        )
    }
}

/** A short hand-inked connector, the kind that links labels on the spell map. */
@Composable
fun GrimoireDashedConnector(modifier: Modifier = Modifier) {
    val gold = WiccanPaletteColors.Grimoire.Border
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.7f)
            quadraticTo(size.width * 0.4f, size.height * 0.05f, size.width * 0.92f, size.height * 0.55f)
        }
        drawPath(path, gold.copy(alpha = 0.85f), style = inkDash(1.4.dp.toPx()))
    }
}

/** Thin ornamental rule with a central flourish, between lesson sections or footers. */
@Composable
fun GrimoireOrnamentalDivider(modifier: Modifier = Modifier, inkOnParchment: Boolean = false) {
    val gold = WiccanPaletteColors.Grimoire.Gold
    val ink = WiccanPaletteColors.Parchment.Ink
    val lineColor = if (inkOnParchment) ink.copy(alpha = 0.42f) else gold.copy(alpha = 0.72f)
    val markColor = if (inkOnParchment) ink.copy(alpha = 0.55f) else WiccanPaletteColors.Grimoire.Border
    Canvas(
        modifier
            .fillMaxWidth()
            .height(16.dp),
    ) {
        val y = size.height / 2f
        val mid = size.width / 2f
        val gap = 14.dp.toPx()
        val stroke = 1.dp.toPx()
        drawLine(lineColor, Offset(0f, y), Offset(mid - gap, y), strokeWidth = stroke)
        drawLine(lineColor, Offset(mid + gap, y), Offset(size.width, y), strokeWidth = stroke)
        val arm = 4.5.dp.toPx()
        val diamond = Path().apply {
            moveTo(mid, y - arm)
            lineTo(mid + arm, y)
            lineTo(mid, y + arm)
            lineTo(mid - arm, y)
            close()
        }
        drawPath(diamond, markColor, style = Stroke(width = stroke))
        drawCircle(markColor, 1.4.dp.toPx(), Offset(mid, y))
    }
}

/** Faint botanical ink and star-chart dots along a parchment leaf's margins. */
@Composable
fun GrimoireParchmentMarginInk(modifier: Modifier = Modifier) {
    val ink = WiccanPaletteColors.Parchment.Ink.copy(alpha = 0.32f)
    val chart = WiccanPaletteColors.Parchment.Border.copy(alpha = 0.38f)
    Canvas(modifier) {
        val stroke = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        val leftVine = Path().apply {
            moveTo(10.dp.toPx(), size.height * 0.12f)
            quadraticTo(18.dp.toPx(), size.height * 0.28f, 11.dp.toPx(), size.height * 0.46f)
            quadraticTo(8.dp.toPx(), size.height * 0.62f, 14.dp.toPx(), size.height * 0.82f)
        }
        drawPath(leftVine, ink, style = stroke)
        val rightVine = Path().apply {
            moveTo(size.width - 12.dp.toPx(), size.height * 0.18f)
            quadraticTo(size.width - 20.dp.toPx(), size.height * 0.34f, size.width - 13.dp.toPx(), size.height * 0.58f)
        }
        drawPath(rightVine, ink, style = stroke)
        val stars = listOf(
            Offset(size.width - 22.dp.toPx(), size.height * 0.24f),
            Offset(size.width - 14.dp.toPx(), size.height * 0.31f),
            Offset(size.width - 28.dp.toPx(), size.height * 0.36f),
            Offset(size.width - 18.dp.toPx(), size.height * 0.44f),
            Offset(size.width - 26.dp.toPx(), size.height * 0.52f),
        )
        stars.forEach { drawCircle(chart, 1.3.dp.toPx(), it) }
        val chartLine = Path().apply {
            moveTo(stars[0].x, stars[0].y)
            lineTo(stars[1].x, stars[1].y)
            lineTo(stars[2].x, stars[2].y)
            lineTo(stars[3].x, stars[3].y)
            lineTo(stars[4].x, stars[4].y)
        }
        drawPath(
            chartLine,
            chart,
            style = Stroke(0.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()), 0f))
        )
    }
}

/** Top-right candle glow only — for screens whose titles must stay unobstructed. */
@Composable
fun GrimoireSettingsHeaderAccent(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    WiccanPaletteColors.Grimoire.Gold.copy(alpha = 0.14f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.92f, size.height * 0.55f),
                radius = size.minDimension * 0.85f,
            ),
            radius = size.minDimension * 0.85f,
            center = Offset(size.width * 0.92f, size.height * 0.55f),
        )
        val vine = Path().apply {
            moveTo(size.width * 0.55f, size.height * 0.08f)
            quadraticTo(size.width * 0.78f, size.height * 0.22f, size.width * 0.98f, size.height * 0.12f)
        }
        drawPath(
            vine,
            WiccanPaletteColors.Grimoire.Border.copy(alpha = 0.28f),
            style = inkDash(1.dp.toPx()),
        )
    }
}

/** Background vine drawn as a dashed ink path plus a corner glow. */
@Composable
fun GrimoireSpellMapWash(modifier: Modifier = Modifier) {
    val gold = WiccanPaletteColors.Grimoire.Border.copy(alpha = 0.45f)
    Canvas(modifier) {
        val stroke = inkDash(1.1.dp.toPx())
        val vine = Path().apply {
            moveTo(0f, size.height * 0.16f)
            quadraticTo(size.width * 0.22f, size.height * 0.02f, size.width * 0.4f, size.height * 0.14f)
            quadraticTo(size.width * 0.62f, size.height * 0.28f, size.width, size.height * 0.08f)
        }
        drawPath(vine, gold, style = stroke)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    WiccanPaletteColors.Grimoire.Gold.copy(alpha = 0.16f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.88f, size.height * 0.06f),
                radius = size.minDimension * 0.28f,
            ),
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.88f, size.height * 0.06f),
        )
    }
}

@Composable
fun GrimoireInkLabel(text: String, modifier: Modifier = Modifier) {
    val ink = WiccanPaletteColors.Grimoire.Text
    val gold = WiccanPaletteColors.Grimoire.Gold
    Text(
        text = text,
        color = ink,
        style = TextStyle(
            fontFamily = WiccanHandwritingFont,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 22.sp,
        ),
        modifier = modifier
            .drawBehind {
                val inset = 1.dp.toPx()
                drawOval(
                    color = gold,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2f, size.height - inset * 2f),
                    style = inkDash(1.15.dp.toPx()),
                )
            }
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/**
 * Eight phases, left to right from new to waning crescent. [active] matches
 * [com.jaimesorrow.wiccan.sky.MoonObservation.trackIndex].
 */
@Composable
fun GrimoireMoonPhases(
    active: Int,
    modifier: Modifier = Modifier,
    activePhaseName: String? = null,
) {
    val description = if (activePhaseName.isNullOrBlank()) {
        stringResource(R.string.motif_moon_phases)
    } else {
        stringResource(R.string.motif_moon_phases_active, activePhaseName)
    }
    val lit = WiccanPaletteColors.Grimoire.Gold
    val shadow = WiccanPaletteColors.Grimoire.Background
    Canvas(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .semantics { contentDescription = description },
    ) {
        val count = 8
        val base = size.height * 0.30f
        for (index in 0 until count) {
            val center = Offset(size.width * (index + 0.5f) / count, size.height / 2f)
            val radius = if (index == active) base * 1.18f else base
            drawMoonPhase(index, center, radius, lit, shadow)
            if (index == active) {
                drawCircle(lit, radius + 2.dp.toPx(), center, style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}

private fun DrawScope.drawMoonPhase(index: Int, center: Offset, radius: Float, lit: Color, shadow: Color) {
    val fillRadius = radius * 0.92f
    val stroke = 1.dp.toPx()
    val border = lit.copy(alpha = 0.85f)
    val moonClip = Path().apply { addOval(Rect(center, radius)) }

    drawCircle(shadow, radius, center)
    drawCircle(border, radius, center, style = Stroke(width = stroke))
    if (index == 0) return
    if (index == 4) {
        drawCircle(lit, fillRadius, center)
        drawCircle(lit.copy(alpha = 0.9f), radius, center, style = Stroke(width = stroke))
        return
    }

    val phaseAngle = index * (PI / 4.0)
    val ellipseRx = (radius * abs(cos(phaseAngle))).toFloat()
    val waxing = index in 1..3
    val litHalf = Path().apply {
        if (waxing) {
            addRect(Rect(center.x, center.y - radius, center.x + radius * 2f, center.y + radius))
        } else {
            addRect(Rect(center.x - radius * 2f, center.y - radius, center.x, center.y + radius))
        }
    }

    clipPath(moonClip) {
        when (index) {
            2, 6 -> {
                clipPath(litHalf) {
                    drawCircle(lit, fillRadius, center)
                }
            }
            3, 5 -> {
                clipPath(litHalf) {
                    drawCircle(lit, fillRadius, center)
                }
                // Terminator ellipse centered on the disk; must not be clipped to the lit half.
                drawOval(
                    color = lit,
                    topLeft = Offset(center.x - ellipseRx, center.y - fillRadius),
                    size = Size(ellipseRx * 2f, fillRadius * 2f),
                )
            }
            else -> {
                clipPath(litHalf) {
                    drawCircle(lit, fillRadius, center)
                    drawOval(
                        color = shadow,
                        topLeft = Offset(center.x - ellipseRx, center.y - fillRadius),
                        size = Size(ellipseRx * 2f, fillRadius * 2f),
                    )
                }
            }
        }
    }
    drawCircle(lit.copy(alpha = 0.9f), radius, center, style = Stroke(width = stroke))
}

/** One phase icon at any size (screenshots / visual checks). */
@Composable
internal fun GrimoireMoonPhaseGlyph(index: Int, modifier: Modifier = Modifier) {
    val lit = WiccanPaletteColors.Grimoire.Gold
    val shadow = WiccanPaletteColors.Grimoire.Background
    Canvas(modifier) {
        val radius = size.minDimension * 0.46f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawMoonPhase(index, center, radius, lit, shadow)
    }
}

/**
 * Thin gold rule on the midnight ground. A bottom banner can sit under this
 * divider without the list above it having to change shape.
 */
@Composable
fun GrimoireBannerSlot(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .testTag("grimoire-banner-slot"),
        thickness = 1.dp,
        color = WiccanPaletteColors.Grimoire.Border,
    )
}

/** Line-art glyphs for the Home section tile row — lesson, wheel, cosmic timing. */
enum class GrimoireHomeSectionGlyph {
    LESSON,
    WHEEL,
    GRIMOIRE,
    SETTINGS,
}

@Composable
fun GrimoireHomeSectionGlyphIcon(
    kind: GrimoireHomeSectionGlyph,
    modifier: Modifier = Modifier,
) {
    val gold = WiccanPaletteColors.Grimoire.Border
    Canvas(modifier.size(36.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
        when (kind) {
            GrimoireHomeSectionGlyph.LESSON -> {
                val radius = 13.dp.toPx()
                drawCircle(gold, radius, center, style = stroke)
                clipPath(Path().apply { addOval(Rect(center, radius)) }) {
                    drawCircle(gold.copy(alpha = 0.25f), radius, Offset(center.x + radius * 0.55f, center.y))
                }
            }
            GrimoireHomeSectionGlyph.WHEEL -> {
                val radius = 12.dp.toPx()
                drawCircle(gold, radius, center, style = stroke)
                repeat(8) { spoke ->
                    val angle = spoke * (Math.PI / 4)
                    val end = Offset(
                        center.x + (radius * kotlin.math.cos(angle)).toFloat(),
                        center.y + (radius * kotlin.math.sin(angle)).toFloat(),
                    )
                    drawLine(gold, center, end, strokeWidth = 1.dp.toPx())
                }
            }
            GrimoireHomeSectionGlyph.GRIMOIRE -> {
                val w = 16.dp.toPx()
                val h = 20.dp.toPx()
                val left = center.x - w / 2f
                val top = center.y - h / 2f
                drawRect(gold, topLeft = Offset(left, top), size = Size(w, h), style = stroke)
                drawLine(gold, Offset(left + w * 0.5f, top), Offset(left + w * 0.5f, top + h), 1.dp.toPx())
            }
            GrimoireHomeSectionGlyph.SETTINGS -> {
                val r = 11.dp.toPx()
                drawCircle(gold, r, center, style = stroke)
                repeat(6) { spoke ->
                    val angle = spoke * (Math.PI / 3)
                    val end = Offset(
                        center.x + (r * 0.85f * kotlin.math.cos(angle)).toFloat(),
                        center.y + (r * 0.85f * kotlin.math.sin(angle)).toFloat(),
                    )
                    drawCircle(gold, 1.6.dp.toPx(), end)
                }
            }
        }
    }
}

@Composable
fun GrimoireHomeSectionTile(
    label: String,
    glyph: GrimoireHomeSectionGlyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val fontScale = LocalDensity.current.fontScale
    val labelSize = if (fontScale > 1.15f) 10.sp else 11.sp
    val labelLineHeight = if (fontScale > 1.15f) 14.sp else 15.sp
    val tileShape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 72.dp, minWidth = 48.dp)
            .clip(tileShape)
            .background(WiccanPaletteColors.Grimoire.CardFill)
            .border(1.dp, WiccanPaletteColors.Grimoire.Border, tileShape)
            .grimoireCornerFlourishes(arm = 8.dp)
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GrimoireHomeSectionGlyphIcon(glyph)
        Text(
            text = label,
            color = WiccanPaletteColors.Grimoire.Gold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = WiccanDisplayFont,
                fontSize = labelSize,
                lineHeight = labelLineHeight,
            ),
        )
    }
}
