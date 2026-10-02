package com.jaimesorrow.wiccan.ui.today.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * Dark-themed skeleton mirroring the loaded layout: a pill row, a title
 * band, a couple of intro lines, and body lines. Gently pulses so it
 * reads as "loading" without a spinner.
 */
@Composable
fun TodaySkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "todaySkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 24.dp, vertical = 32.dp))
            .alpha(alpha),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Placeholder(widthFraction = 0.30f, height = 20.dp, corner = 50.dp)
        Placeholder(widthFraction = 0.85f, height = 36.dp)
        Placeholder(widthFraction = 0.65f, height = 36.dp)
        Placeholder(widthFraction = 0.9f, height = 18.dp)
        Placeholder(widthFraction = 0.7f, height = 18.dp)
        HairlineDivider(modifier = Modifier.padding(vertical = 8.dp))
        repeat(4) { Placeholder(widthFraction = 1f, height = 14.dp) }
        Placeholder(widthFraction = 0.55f, height = 14.dp)
    }
}

@Composable
private fun Placeholder(widthFraction: Float, height: androidx.compose.ui.unit.Dp, corner: androidx.compose.ui.unit.Dp = 6.dp) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(corner)),
    )
}
