package com.jaimesorrow.wiccan.ui.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wiccan.app.R

/** A soft gold wash that keeps the page opaque and uses the selected palette as its base. */
@Composable
internal fun Modifier.wiccanPageWash(): Modifier {
    val baseColor = MaterialTheme.colorScheme.background
    val strength = if (baseColor.luminance() > 0.5f) 0.04f else 0.08f
    val glow = wiccanEmblemColor().copy(alpha = strength).compositeOver(baseColor)
    return background(Brush.verticalGradient(0f to glow, 0.60f to baseColor, 1f to baseColor))
}

@Composable
internal fun WiccanBotanicalAccent(modifier: Modifier = Modifier, width: Dp = 132.dp) {
    Image(
        painter = painterResource(R.drawable.wiccan_botanical_accent),
        contentDescription = null,
        colorFilter = ColorFilter.tint(wiccanEmblemColor()),
        modifier = modifier.size(width = width, height = 16.dp),
    )
}
