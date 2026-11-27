package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val CardShape = RoundedCornerShape(6.dp)

@Composable
fun GrimoireFloralBackdrop(modifier: Modifier = Modifier) {
    val gold = WiccanPaletteColors.Grimoire.Border.copy(alpha = 0.55f)
    Canvas(modifier) {
        val stroke = Stroke(
            width = 1.1.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx()), 0f),
        )
        val vine = Path().apply {
            moveTo(0f, size.height * 0.18f)
            quadraticTo(size.width * 0.18f, size.height * 0.02f, size.width * 0.34f, size.height * 0.2f)
            quadraticTo(size.width * 0.55f, size.height * 0.42f, size.width, size.height * 0.1f)
        }
        drawPath(vine, gold, style = stroke)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(WiccanPaletteColors.Grimoire.Gold.copy(alpha = 0.20f), WiccanPaletteColors.Grimoire.Gold.copy(alpha = 0f)),
                center = Offset(size.width * 0.86f, size.height * 0.08f),
                radius = 72.dp.toPx(),
            ),
            radius = 72.dp.toPx(),
            center = Offset(size.width * 0.86f, size.height * 0.08f),
        )
    }
}

@Composable
fun grimoireFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = WiccanPaletteColors.Grimoire.Border,
    unfocusedBorderColor = WiccanPaletteColors.Grimoire.Border,
    focusedLabelColor = WiccanPaletteColors.Grimoire.Gold,
    unfocusedLabelColor = WiccanPaletteColors.Grimoire.Gold,
    cursorColor = WiccanPaletteColors.Grimoire.Gold,
    focusedTextColor = WiccanPaletteColors.Grimoire.Text,
    unfocusedTextColor = WiccanPaletteColors.Grimoire.Text,
)

@Composable
fun grimoireSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = WiccanPaletteColors.Grimoire.Gold,
    checkedThumbColor = WiccanPaletteColors.Grimoire.OnGold,
    uncheckedTrackColor = WiccanPaletteColors.Grimoire.Raised,
    uncheckedBorderColor = WiccanPaletteColors.Grimoire.Border,
    uncheckedThumbColor = WiccanPaletteColors.Grimoire.Gold,
)

@Composable
fun GrimoireButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(WiccanPaletteColors.Grimoire.Border, WiccanPaletteColors.Grimoire.Gold),
                ),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = WiccanPaletteColors.Grimoire.OnGold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun GrimoireSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(top = 22.dp, bottom = 8.dp).semantics { heading() },
        color = WiccanPaletteColors.Grimoire.Gold,
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = WiccanDisplayFont),
    )
}

@Composable
fun GrimoireChevronRow(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("›", color = WiccanPaletteColors.Grimoire.Gold, style = MaterialTheme.typography.titleMedium)
    }
}

fun Modifier.grimoireCard(): Modifier = this
    .clip(CardShape)
    .background(WiccanPaletteColors.Grimoire.CardFill)
    .border(1.dp, WiccanPaletteColors.Grimoire.Border, CardShape)

@Composable
fun GrimoireStage(content: @Composable () -> Unit) {
    MidnightGrimoireTheme {
        Box(Modifier.fillMaxSize().background(WiccanPaletteColors.Grimoire.Background)) {
            GrimoireFloralBackdrop(Modifier.fillMaxSize())
            content()
        }
    }
}
