package com.jaimesorrow.wiccan.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import com.jaimesorrow.wiccan.ui.theme.WiccanEditorialFont
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.jaimesorrow.wiccan.ui.theme.grimoireCornerFlourishes
import com.jaimesorrow.wiccan.ui.today.components.ContentTypePill

/** One row in the Archive list: type badge, title, tradition, delivered date. */
@Composable
fun ArchiveEntryRow(entry: HistoryEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape)
            .background(WiccanPaletteColors.Grimoire.CardFill)
            .border(1.dp, WiccanPaletteColors.Grimoire.Border, shape)
            .grimoireCornerFlourishes()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContentTypePill(entry.type)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = entry.deliveredDate,
                style = MaterialTheme.typography.labelSmall,
                color = WiccanPaletteColors.Grimoire.Muted,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = WiccanEditorialFont),
            color = WiccanPaletteColors.Grimoire.Text,
        )
        Text(
            text = entry.tradition,
            style = MaterialTheme.typography.bodyMedium,
            color = WiccanPaletteColors.Grimoire.Muted,
        )
    }
}
