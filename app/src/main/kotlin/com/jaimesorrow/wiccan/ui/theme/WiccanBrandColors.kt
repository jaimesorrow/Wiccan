package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class WiccanBrandColors(
    val brandGold: Color,
)

internal val LocalWiccanBrandColors = staticCompositionLocalOf {
    WiccanBrandColors(brandGold = WiccanPalette.NEAR_BLACK.brandGold)
}
