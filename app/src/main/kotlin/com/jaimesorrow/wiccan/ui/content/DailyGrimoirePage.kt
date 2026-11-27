package com.jaimesorrow.wiccan.ui.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.sky.CelestialCalculator
import com.jaimesorrow.wiccan.ui.theme.GrimoireCandleGlow
import com.jaimesorrow.wiccan.ui.theme.GrimoireMoonPhases
import com.jaimesorrow.wiccan.ui.theme.GrimoireOrnamentalDivider
import com.jaimesorrow.wiccan.ui.theme.GrimoireParchmentMarginInk
import com.jaimesorrow.wiccan.ui.theme.WiccanDisplayFont
import com.jaimesorrow.wiccan.ui.theme.WiccanEditorialFont
import com.jaimesorrow.wiccan.ui.theme.WiccanHandwritingFont
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.wiccan.app.R
import java.time.Instant
import java.util.Locale
private val Ink = WiccanPaletteColors.Parchment.Ink
private val Midnight = WiccanPaletteColors.Grimoire.Background
private val PageShape = RoundedCornerShape(6.dp)

/**
 * Today's page, laid out like an open grimoire leaf: calligraphy title,
 * italic editorial prose, a candle at the top right and a quill at the bottom left.
 * Safety, source, and rights stay in the plain body face.
 *
 * When [fillViewport] is true (default), the parchment leaf grows to consume the
 * space below the moon row so short lessons do not leave a void on the wood
 * background. Long copy scrolls inside the leaf instead of the outer column.
 */
@Composable
internal fun DailyGrimoirePage(
    content: Content,
    modifier: Modifier = Modifier,
    onOpenGrimoire: (() -> Unit)? = null,
    fillViewport: Boolean = true,
) {
    val moon = remember { CelestialCalculator.moon(Instant.now()) }
    val pageModifier =
        if (fillViewport) {
            modifier.fillMaxSize()
        } else {
            modifier.fillMaxWidth().wrapContentHeight()
        }
    val woodPainter = painterResource(R.drawable.grimoire_wood)
    Box(pageModifier) {
        if (fillViewport) {
            Image(
                painter = woodPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(Modifier.matchParentSize().background(Midnight.copy(alpha = 0.42f)))
        }
        Column(
            modifier = Modifier
                .then(
                    if (fillViewport) {
                        Modifier.matchParentSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .paint(woodPainter, contentScale = ContentScale.Crop)
                            .background(Midnight.copy(alpha = 0.42f))
                    },
                )
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GrimoireMoonPhases(active = moon.trackIndex, activePhaseName = moon.name)
            Text(
                text = stringResource(R.string.motif_lesson_margin),
                color = WiccanPaletteColors.Grimoire.Text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = WiccanHandwritingFont,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                ),
            )
            if (fillViewport) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                ) {
                    LessonLeaf(
                        content = content,
                        onOpenGrimoire = onOpenGrimoire,
                        modifier = Modifier.fillMaxSize(),
                        scrollInsideLeaf = true,
                    )
                }
            } else {
                LessonLeaf(
                    content = content,
                    onOpenGrimoire = onOpenGrimoire,
                    modifier = Modifier.fillMaxWidth(),
                    scrollInsideLeaf = false,
                )
            }
        }
    }
}

@Composable
private fun LessonLeaf(
    content: Content,
    modifier: Modifier = Modifier,
    onOpenGrimoire: (() -> Unit)?,
    scrollInsideLeaf: Boolean,
) {
    val prose = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = WiccanEditorialFont,
        fontStyle = FontStyle.Italic,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = Ink,
    )
    val scrollState = rememberScrollState()
    val fontScale = LocalDensity.current.fontScale
    val horizontalInset = 18.dp
    val candleReserveEnd = 96.dp * fontScale
    val pageMargins = Modifier.padding(
        start = horizontalInset,
        end = horizontalInset,
        top = 18.dp,
        bottom = 24.dp,
    )
    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.grimoire_parchment),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().clip(PageShape),
        )
        GrimoireParchmentMarginInk(Modifier.matchParentSize().clip(PageShape))
        Box(
            Modifier
                .matchParentSize()
                .clip(PageShape)
                .background(WiccanPaletteColors.Parchment.Background.copy(alpha = 0.72f))
                .border(1.dp, WiccanPaletteColors.Parchment.Border, PageShape),
        )
        Column(
            modifier = Modifier
                .then(
                    if (scrollInsideLeaf) {
                        Modifier
                            .matchParentSize()
                            .verticalScroll(scrollState)
                    } else {
                        Modifier.fillMaxWidth()
                    },
                )
                .then(pageMargins),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LessonTitleHeader(
                title = content.title,
                candleReserveEnd = candleReserveEnd,
            )
            Text(
                text = stringResource(R.string.today_lesson),
                color = Ink,
                style = prose.copy(fontSize = 20.sp, lineHeight = 26.sp),
            )
            Text(
                text = stringResource(R.string.today_subtitle),
                color = Ink,
                style = prose,
            )
            val tools = content.tools.ifEmpty { content.materials.orEmpty() }
            var step = 1
            if (tools.isNotEmpty()) {
                GrimoireOrnamentalDivider(inkOnParchment = true)
                SectionTitle(stringResource(R.string.content_materials))
                tools.forEach { item ->
                    Text("☐  $item", color = Ink, style = prose)
                }
                GrimoireOrnamentalDivider(inkOnParchment = true)
            }
            if (content.intro.isNotBlank()) {
                Text(content.intro, color = Ink, style = prose)
            }
            LessonRomanSection(step = step++, label = stringResource(R.string.content_method), body = content.body, prose = prose)
            content.reflection?.takeIf { it.isNotBlank() }?.let {
                LessonRomanSection(
                    step = step++,
                    label = stringResource(R.string.content_reflection),
                    body = it,
                    prose = prose,
                )
            }
            content.practice?.takeIf { it.isNotBlank() }?.let {
                LessonRomanSection(
                    step = step,
                    label = stringResource(R.string.content_practice),
                    body = it,
                    prose = prose,
                )
            }
            LessonClosingBlock(
                safetyNote = content.safetyNote,
                onOpenGrimoire = onOpenGrimoire,
            )
            content.sourceCitation?.takeIf { it.isNotBlank() }?.let {
                LessonMutedCopy(title = stringResource(R.string.content_source), body = it)
            }
            content.rightsNotes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = mutedLessonInk(),
                    style = lessonMutedTextStyle(MaterialTheme.typography.bodySmall),
                )
            }
            QuillAndInkpotOrnament(
                Modifier
                    .padding(top = 16.dp)
                    .size(width = 116.dp, height = 84.dp)
                    .testTag("lesson_quill_ornament"),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun LessonTitleHeader(title: String, candleReserveEnd: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
    ) {
        val candleDescription = stringResource(R.string.lesson_candle_description)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp)
                .size(104.dp),
        ) {
            GrimoireCandleGlow(Modifier.fillMaxSize())
        }
        Image(
            painter = painterResource(R.drawable.grimoire_candle),
            contentDescription = candleDescription,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp)
                .size(width = 88.dp, height = 96.dp),
        )
        Text(
            text = title,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = candleReserveEnd, top = 8.dp, bottom = 4.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = WiccanDisplayFont,
                fontSize = 40.sp,
                lineHeight = 50.sp,
            ),
        )
    }
}

@Composable
private fun LessonRomanSection(step: Int, label: String, body: String, prose: androidx.compose.ui.text.TextStyle) {
    GrimoireOrnamentalDivider(inkOnParchment = true)
    SectionTitle("${roman(step)}. $label")
    Text(body, color = Ink, style = prose)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(Locale.ROOT),
        color = WiccanPaletteColors.Grimoire.Gold,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = WiccanDisplayFont,
            letterSpacing = 1.4.sp,
        ),
    )
}

@Composable
private fun LessonClosingBlock(safetyNote: String?, onOpenGrimoire: (() -> Unit)?) {
    if (safetyNote.isNullOrBlank() && onOpenGrimoire == null) return
    GrimoireOrnamentalDivider(inkOnParchment = true, modifier = Modifier.padding(top = 6.dp))
    safetyNote?.takeIf { it.isNotBlank() }?.let { note ->
        LessonMutedCopy(title = stringResource(R.string.content_safety_heading), body = note)
    }
    Text(
        stringResource(R.string.content_saved),
        color = mutedLessonInk(),
        style = lessonMutedTextStyle(MaterialTheme.typography.bodyMedium),
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
    onOpenGrimoire?.let { action ->
        TextButton(onClick = action) {
            Text(
                stringResource(R.string.today_open_grimoire),
                color = mutedLessonInk(),
                style = lessonMutedTextStyle(MaterialTheme.typography.bodyMedium),
            )
        }
    }
}

@Composable
private fun LessonMutedCopy(title: String, body: String) {
    Text(
        text = title,
        color = mutedLessonInk(),
        modifier = Modifier.semantics { heading() },
        style = lessonMutedTextStyle(MaterialTheme.typography.bodyMedium),
    )
    Text(
        text = body,
        color = mutedLessonInk(),
        modifier = Modifier.padding(top = 2.dp),
        style = lessonMutedTextStyle(MaterialTheme.typography.bodyMedium),
    )
}

@Composable
private fun mutedLessonInk() = Ink.copy(alpha = 0.78f)

@Composable
private fun lessonMutedTextStyle(base: androidx.compose.ui.text.TextStyle) = base.copy(
    fontFamily = WiccanEditorialFont,
    fontWeight = FontWeight.Normal,
    fontStyle = FontStyle.Normal,
    lineHeight = 24.sp,
)

private fun roman(value: Int): String {
    val numerals = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
    return numerals.getOrElse(value - 1) { value.toString() }
}
