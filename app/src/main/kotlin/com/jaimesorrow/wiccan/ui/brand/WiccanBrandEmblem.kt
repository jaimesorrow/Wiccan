package com.jaimesorrow.wiccan.ui.brand

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import com.wiccan.app.R

/**
 * First approved Wiccan brand identity: the W / flame / diamond seal.
 * The adaptive vector remains gold on Midnight Blue. In Compose, the
 * seal uses dark primary on Light/Ivory to maintain contrast.
 */
@Composable
fun WiccanBrandEmblem(modifier: Modifier = Modifier) {
    val color = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        Color(0xFF7C6A48)
    } else {
        Color(0xFFE8BC61)
    }
    Icon(
        painter = painterResource(id = R.drawable.ic_launcher_foreground),
        contentDescription = "Wiccan seal",
        tint = color,
        modifier = modifier,
    )
}
