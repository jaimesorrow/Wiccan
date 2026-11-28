package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.billing.formatPromoCodeForPlay
import com.wiccan.app.R

/**
 * Small plain entry on the paywall (including post-onboarding subscription).
 * Expansion and input stay local. This link supports generated one-time-use
 * Play promotion codes; custom codes must be entered in Play's purchase sheet.
 */
@Composable
fun PromoCodeSection(
    onRedeem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    val redeemEnabled = formatPromoCodeForPlay(code).isNotEmpty()
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.testTag("paywall-promo-toggle"),
        ) {
            Text(
                stringResource(R.string.paywall_promo_toggle),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).testTag("paywall-promo-expanded"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.paywall_promo_hint)) },
                    modifier = Modifier.fillMaxWidth().testTag("paywall-promo-field"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (redeemEnabled) onRedeem(code) },
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onRedeem(code) },
                    enabled = redeemEnabled,
                    modifier = Modifier.fillMaxWidth().testTag("paywall-promo-redeem"),
                ) {
                    Text(stringResource(R.string.paywall_promo_redeem))
                }
            }
        }
    }
}
