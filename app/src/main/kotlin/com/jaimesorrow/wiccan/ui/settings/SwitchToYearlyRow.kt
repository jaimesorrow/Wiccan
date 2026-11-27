package com.jaimesorrow.wiccan.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.domain.model.YearlySwitchOffer
import com.wiccan.app.R

/**
 * Isolated Settings row for switching an active monthly subscription to
 * yearly. Kept in its own file so it stays easy to rebase against the
 * Midnight Grimoire restyle in draft PR #226, which also touches Settings
 * strings but not this composable.
 */
@Composable
fun SwitchToYearlyRow(
    offer: YearlySwitchOffer,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().testTag("switch-to-yearly")) {
        val introPrice = offer.introFormattedPrice
        val terms = if (introPrice != null) {
            stringResource(
                R.string.settings_switch_to_yearly_intro,
                introPrice,
                offer.yearlyFormattedPrice,
            )
        } else {
            stringResource(
                R.string.settings_switch_to_yearly_base,
                offer.yearlyFormattedPrice,
            )
        }
        Text(
            text = terms,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().testTag("switch-to-yearly-button"),
            enabled = enabled,
        ) {
            Text(stringResource(R.string.settings_switch_to_yearly))
        }
    }
}
