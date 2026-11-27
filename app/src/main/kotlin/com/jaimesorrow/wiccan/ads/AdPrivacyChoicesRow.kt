package com.jaimesorrow.wiccan.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiccan.app.R

/**
 * Settings entry that re-opens the UMP privacy options form. Shown only when
 * UMP reports the entry point is required (for example in the EEA, UK or
 * Switzerland, or a U.S. state with a privacy law).
 */
@Composable
fun AdPrivacyChoicesRow(viewModel: AdsViewModel = hiltViewModel()) {
    val tierAllowsAds by viewModel.tierAllowsAds.collectAsStateWithLifecycle()
    val required by viewModel.privacyOptionsRequired.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    LaunchedEffect(tierAllowsAds, activity) {
        // Refreshes UMP's requirement status for non-subscribers.
        if (tierAllowsAds && activity != null) viewModel.gatherConsent(activity)
    }
    if (required && activity != null) {
        PrivacyChoicesRowContent(onClick = { viewModel.showPrivacyOptions(activity) })
    }
}

/** Same layout and type styles as the Settings analytics row. */
@Composable
internal fun PrivacyChoicesRowContent(onClick: () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("settings-privacy-choices"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = stringResource(R.string.settings_ad_privacy_choices),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.settings_ad_privacy_choices_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
