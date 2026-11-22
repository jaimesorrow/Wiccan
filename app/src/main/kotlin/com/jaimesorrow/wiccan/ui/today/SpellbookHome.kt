package com.jaimesorrow.wiccan.ui.today

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.ui.archive.displayName
import com.jaimesorrow.wiccan.ui.brand.WiccanFullLogo
import com.jaimesorrow.wiccan.ui.brand.wiccanEmblemColor
import com.wiccan.app.R

private val ParchmentInk = Color(0xFF33200E)
private val FrameGold = Color(0xFFC5913B)
private val ButtonGold = Color(0xFFE8BC61)

@Composable
internal fun SpellbookHome(
    content: Content,
    onOpenToday: () -> Unit,
    onOpenGrimoire: () -> Unit,
) {
    val accent = wiccanEmblemColor()
    val heroShape = RoundedCornerShape(18.dp)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WiccanFullLogo(width = 144.dp, modifier = Modifier.padding(bottom = 4.dp))
            Image(
                painter = painterResource(R.drawable.wiccan_home_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.48f)
                    .clip(heroShape)
                    .border(1.dp, FrameGold, heroShape),
            )
            DailySpellCard(content = content, onOpenToday = onOpenToday)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = accent.copy(alpha = 0.5f))
                TextButton(onClick = onOpenGrimoire, modifier = Modifier.weight(4f)) {
                    Text(
                        text = stringResource(R.string.today_open_grimoire),
                        color = accent,
                        textAlign = TextAlign.Center,
                    )
                }
                HorizontalDivider(modifier = Modifier.weight(1f), color = accent.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun DailySpellCard(content: Content, onOpenToday: () -> Unit) {
    val sectionLabel = when (content.type) {
        ContentType.SPELL -> R.string.today_spell
        ContentType.LESSON -> R.string.today_lesson
        ContentType.POTION -> R.string.today_potion
        ContentType.UNKNOWN -> R.string.home_today_practice
    }
    val actionLabel = when (content.type) {
        ContentType.SPELL -> R.string.home_open_spell
        ContentType.LESSON -> R.string.home_open_lesson
        ContentType.POTION -> R.string.home_open_potion
        ContentType.UNKNOWN -> R.string.home_open_practice
    }
    val outerShape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFE8C693), Color(0xFFF8E7C2), Color(0xFFE5BE85))),
                outerShape,
            )
            .border(1.5.dp, FrameGold, outerShape)
            .padding(4.dp)
            .border(1.dp, ParchmentInk.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = ParchmentInk.copy(alpha = 0.45f))
            Text(
                text = stringResource(sectionLabel),
                modifier = Modifier.weight(3f),
                style = MaterialTheme.typography.labelLarge,
                color = ParchmentInk,
                textAlign = TextAlign.Center,
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = ParchmentInk.copy(alpha = 0.45f))
        }
        Text(
            text = content.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall,
            color = ParchmentInk,
            textAlign = TextAlign.Center,
        )
        Text(
            text = content.category.displayName(),
            style = MaterialTheme.typography.bodyMedium,
            color = ParchmentInk,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onOpenToday,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGold, contentColor = ParchmentInk),
        ) {
            Text(
                text = stringResource(actionLabel),
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Icon(
                painter = painterResource(R.drawable.ic_home_arrow),
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
