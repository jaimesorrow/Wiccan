package com.jaimesorrow.wiccan.ui.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.wiccan.app.R

// Brand marks, both extracted unchanged from the approved artwork in
// brand/source/ (see brand/README.md):
//  - WiccanEmblem: the app-icon seal (circle, W, flame, diamond), for
//    content headers and compact identity surfaces;
//  - WiccanFullLogo: the full logo (thinner emblem + WICCAN wordmark),
//    for sign-in, loading and other major identity surfaces.

/** True on the Light/Ivory palette (and any light blend of it). */
@Composable
@ReadOnlyComposable
private fun isLightBackground(): Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f

/** Gold #E8BC61 on dark palettes; the Light/Ivory dark primary on light ones, per the brand guidelines. */
@Composable
@ReadOnlyComposable
fun wiccanEmblemColor(): Color =
    if (isLightBackground()) WiccanPaletteColors.LightIvory.Primary else WiccanPaletteColors.Brand.EmblemGold

@Composable
fun WiccanEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(R.drawable.wiccan_emblem),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(wiccanEmblemColor()),
        modifier = modifier.size(size),
    )
}

/**
 * Original colours on dark palettes. On Light/Ivory the same shapes are
 * drawn in the dark primary so the cream wordmark stays legible.
 */
@Composable
fun WiccanFullLogo(
    modifier: Modifier = Modifier,
    width: Dp = 200.dp,
    contentDescription: String? = "Wiccan",
) {
    val light = isLightBackground()
    Image(
        painter = painterResource(if (light) R.drawable.wiccan_full_logo_mask else R.drawable.wiccan_full_logo),
        contentDescription = contentDescription,
        colorFilter = if (light) ColorFilter.tint(WiccanPaletteColors.LightIvory.Primary) else null,
        contentScale = ContentScale.Fit,
        modifier = modifier.width(width),
    )
}
