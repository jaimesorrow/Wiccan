package com.jaimesorrow.wiccan.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.wiccan.app.R

/** Compose stand-in of the Glance widget, using the same copy and Grimoire colors. */
@Composable
internal fun WiccanWidgetPreview() {
    val colorScheme = WiccanPaletteColors.Grimoire
    Column(
        modifier = Modifier.fillMaxSize().background(colorScheme.Background).padding(16.dp),
    ) {
        Text(text = stringResource(R.string.widget_title), color = colorScheme.Gold, fontSize = 20.sp)
        Text(text = stringResource(R.string.widget_subtitle), color = colorScheme.Text, fontSize = 14.sp)
    }
}
