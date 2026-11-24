package com.jaimesorrow.wiccan.ui.content

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jaimesorrow.wiccan.ui.brand.wiccanEmblemColor
import com.wiccan.app.R

private val ParchmentInk = Color(0xFF382311)
private val ParchmentBorder = Color(0xFF986A30)
private val Parchment = listOf(Color(0xFFF1D4A4), Color(0xFFFFEDCA), Color(0xFFEFD2A2))

@Composable
internal fun IncantationCard(text: String) {
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(Parchment), cardShape)
            .border(1.dp, ParchmentBorder, cardShape)
            .padding(4.dp)
            .border(1.dp, ParchmentBorder.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FloralLessonHeading(
            title = stringResource(R.string.content_incantation),
            initial = 'I',
            artwork = R.drawable.wiccan_floral_initial_i,
            artworkWidthEm = 0.78f,
            color = ParchmentInk,
            textAlign = TextAlign.Center,
        )
        SelectionContainer {
            Text(
                text = text,
                color = ParchmentInk,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                ),
            )
        }
    }
}

@Composable
internal fun ReflectionSection(text: String, color: Color? = null) {
    val accent = wiccanEmblemColor()
    val ink = color ?: MaterialTheme.colorScheme.onBackground
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.wiccan_triple_moon_divider),
            contentDescription = null,
            colorFilter = ColorFilter.tint(accent),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .aspectRatio(960f / 83f),
        )
        FloralLessonHeading(
            title = stringResource(R.string.content_reflection),
            initial = 'R',
            artwork = R.drawable.wiccan_floral_initial_r,
            artworkWidthEm = 1.4f,
            color = ink,
        )
        Text(text, style = MaterialTheme.typography.bodyLarge, color = ink)
    }
}

@Composable
private fun FloralLessonHeading(
    title: String,
    initial: Char,
    @DrawableRes artwork: Int,
    artworkWidthEm: Float,
    color: Color,
    textAlign: TextAlign = TextAlign.Start,
) {
    val modifier = Modifier.fillMaxWidth().semantics { heading() }
    val style = MaterialTheme.typography.headlineSmall
    if (title.firstOrNull() != initial) {
        Text(title, modifier = modifier, style = style, color = color, textAlign = textAlign)
        return
    }

    // The original first letter is the inline artwork's alternate text, so the heading
    // stays intact for accessibility. Em units let the ornament scale with the text.
    val label = buildAnnotatedString {
        appendInlineContent("floral-initial", alternateText = title.take(1))
        append(title.drop(1))
    }
    Text(
        text = label,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        inlineContent = mapOf(
            "floral-initial" to InlineTextContent(
                Placeholder(artworkWidthEm.em, 1.4.em, PlaceholderVerticalAlign.AboveBaseline),
            ) {
                Image(
                    painter = painterResource(artwork),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(end = 3.dp),
                )
            },
        ),
    )
}
