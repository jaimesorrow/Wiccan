package com.jaimesorrow.wiccan.ui.content

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.sky.CelestialCalculator
import com.jaimesorrow.wiccan.ui.brand.WiccanLineArt
import com.jaimesorrow.wiccan.ui.brand.WiccanLineArtKind
import com.jaimesorrow.wiccan.ui.theme.GrimoireCandleGlow
import com.jaimesorrow.wiccan.ui.theme.GrimoireMoonPhases
import com.jaimesorrow.wiccan.ui.theme.GrimoireOrnamentalDivider
import com.jaimesorrow.wiccan.ui.theme.GrimoireSettingsHeaderAccent
import com.jaimesorrow.wiccan.ui.theme.WiccanDisplayFont
import com.jaimesorrow.wiccan.ui.theme.WiccanGrimoireStepsFont
import com.jaimesorrow.wiccan.ui.theme.WiccanHandwritingFont
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.wiccan.app.R
import java.time.Instant
import java.util.Locale

private val Ink = WiccanPaletteColors.Parchment.Ink
private val Midnight = WiccanPaletteColors.Grimoire.Background
private val Gold = WiccanPaletteColors.Grimoire.Gold
private val ShellText = WiccanPaletteColors.Grimoire.Text
private val ShellMuted = WiccanPaletteColors.Grimoire.Muted

/**
 * Today's lesson: dark grimoire shell (titles, materials, safety) with the numbered
 * Working step on tattered parchment in Pinyon Script; reflection and practice use the dark shell.
 */
@Composable
internal fun DailyGrimoirePage(
    content: Content,
    modifier: Modifier = Modifier,
    onOpenGrimoire: (() -> Unit)? = null,
    fillViewport: Boolean = true,
) {
    val moon = remember { CelestialCalculator.moon(Instant.now()) }
    val scrollState = rememberScrollState()
    val pageModifier =
        if (fillViewport) {
            modifier.fillMaxSize()
        } else {
            modifier.fillMaxWidth().wrapContentHeight()
        }
    Box(
        pageModifier.background(Midnight),
    ) {
        GrimoireSettingsHeaderAccent(
            Modifier
                .align(Alignment.TopEnd)
                .size(width = 160.dp, height = 88.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (fillViewport) {
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    } else {
                        Modifier.wrapContentHeight()
                    },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GrimoireMoonPhases(active = moon.trackIndex, activePhaseName = moon.name)
            Text(
                text = stringResource(R.string.motif_lesson_margin),
                color = ShellMuted,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = WiccanHandwritingFont,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                ),
            )
            LessonShellContent(
                content = content,
                onOpenGrimoire = onOpenGrimoire,
            )
        }
    }
}

@Composable
private fun LessonShellContent(
    content: Content,
    onOpenGrimoire: (() -> Unit)?,
) {
    val fontScale = LocalDensity.current.fontScale
    val candleReserveEnd = 96.dp * fontScale
    val body = MaterialTheme.typography.bodyLarge.copy(color = ShellText, lineHeight = 26.sp)
    val stepStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = WiccanGrimoireStepsFont,
        fontSize = 22.sp,
        lineHeight = 34.sp,
        color = Ink,
    )
    val tools = content.tools.ifEmpty { content.materials.orEmpty() }
    var step = 1
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LessonTitleHeader(title = content.title, candleReserveEnd = candleReserveEnd)
        Text(
            text = stringResource(R.string.today_lesson),
            color = Gold,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = WiccanDisplayFont),
        )
        Text(
            text = stringResource(R.string.today_subtitle),
            color = ShellMuted,
            style = body,
        )
        if (tools.isNotEmpty()) {
            GrimoireOrnamentalDivider()
            ShellSectionTitle(stringResource(R.string.content_materials))
            tools.forEach { item ->
                Text("☐  $item", color = ShellText, style = body)
            }
        }
        if (content.intro.isNotBlank()) {
            GrimoireOrnamentalDivider()
            Text(content.intro, color = ShellText, style = body)
        }
        if (content.body.isNotBlank()) {
            GrimoireOrnamentalDivider(modifier = Modifier.padding(top = 4.dp))
            LessonTatteredParchment(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lesson_steps_parchment"),
                seed = content.id.hashCode(),
            ) {
                ParchmentWorkingSection(
                    step = step++,
                    label = stringResource(R.string.content_method),
                    body = content.body,
                    stepStyle = stepStyle,
                )
            }
        }
        content.reflection?.takeIf { it.isNotBlank() }?.let {
            ShellStepSection(
                step = step++,
                label = stringResource(R.string.content_reflection),
                body = it,
                bodyStyle = body,
            )
        }
        content.practice?.takeIf { it.isNotBlank() }?.let {
            ShellStepSection(
                step = step,
                label = stringResource(R.string.content_practice),
                body = it,
                bodyStyle = body,
            )
        }
        LessonClosingBlock(
            safetyNote = content.safetyNote,
            onOpenGrimoire = onOpenGrimoire,
        )
        content.sourceCitation?.takeIf { it.isNotBlank() }?.let {
            ShellMutedCopy(title = stringResource(R.string.content_source), body = it)
        }
        content.rightsNotes?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = ShellMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        QuillAndInkpotOrnament(
            Modifier
                .padding(top = 8.dp)
                .size(width = 116.dp, height = 84.dp)
                .testTag("lesson_quill_ornament"),
        )
        Spacer(Modifier.height(24.dp))
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
                .size(104.dp)
                .clip(CircleShape),
        ) {
            GrimoireCandleGlow(Modifier.fillMaxSize())
        }
        WiccanLineArt(
            kind = WiccanLineArtKind.Candle,
            contentDescription = candleDescription,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp)
                .size(96.dp)
                .clip(CircleShape),
        )
        Text(
            text = title,
            color = Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = candleReserveEnd, top = 8.dp, bottom = 4.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = WiccanDisplayFont,
                fontSize = 36.sp,
                lineHeight = 44.sp,
            ),
        )
    }
}

@Composable
private fun ParchmentWorkingSection(
    step: Int,
    label: String,
    body: String,
    stepStyle: androidx.compose.ui.text.TextStyle,
) {
    ParchmentSectionTitle("${roman(step)}. $label")
    Text(
        body,
        color = Ink,
        style = stepStyle,
        modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
    )
}

@Composable
private fun ShellStepSection(
    step: Int,
    label: String,
    body: String,
    bodyStyle: androidx.compose.ui.text.TextStyle,
) {
    GrimoireOrnamentalDivider(modifier = Modifier.padding(top = 8.dp))
    ShellSectionTitle("${roman(step)}. $label")
    Text(
        body,
        color = ShellText,
        style = bodyStyle,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun ShellSectionTitle(text: String) {
    Text(
        text = text.uppercase(Locale.ROOT),
        color = Gold,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = WiccanDisplayFont,
            letterSpacing = 1.2.sp,
        ),
    )
}

@Composable
private fun ParchmentSectionTitle(text: String) {
    Text(
        text = text.uppercase(Locale.ROOT),
        color = Ink,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleSmall.copy(
            fontFamily = WiccanGrimoireStepsFont,
            fontSize = 20.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.4.sp,
        ),
    )
}

@Composable
private fun LessonClosingBlock(safetyNote: String?, onOpenGrimoire: (() -> Unit)?) {
    if (safetyNote.isNullOrBlank() && onOpenGrimoire == null) return
    GrimoireOrnamentalDivider(modifier = Modifier.padding(top = 8.dp))
    safetyNote?.takeIf { it.isNotBlank() }?.let { note ->
        Text(
            text = stringResource(R.string.content_safety_heading),
            color = ShellText,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = note,
            color = ShellText,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Text(
        stringResource(R.string.content_saved),
        color = ShellText,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 12.dp).semantics { heading() },
    )
    onOpenGrimoire?.let { action ->
        TextButton(onClick = action) {
            Text(
                stringResource(R.string.today_open_grimoire),
                color = Gold,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ShellMutedCopy(title: String, body: String) {
    Text(
        text = title,
        color = ShellMuted,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    )
    Text(
        text = body,
        color = ShellMuted,
        modifier = Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun roman(value: Int): String {
    val numerals = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
    return numerals.getOrElse(value - 1) { value.toString() }
}
