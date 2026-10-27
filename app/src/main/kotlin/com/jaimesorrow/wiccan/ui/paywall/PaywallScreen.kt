package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.core.util.findActivity
import com.jaimesorrow.wiccan.ui.today.components.CrescentGlyph
import kotlinx.coroutines.awaitCancellation

/** Payment is exclusively Google Play Billing; access is verified by the root gate. */
@Composable
fun PaywallScreen(
    modifier: Modifier = Modifier,
    onAccountClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshOffer()
            awaitCancellation()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PaywallUiEffect.ShowSnackbar -> snackbars.showSnackbar(effect.error.toUserMessage())
                PaywallUiEffect.PurchasesRestored -> snackbars.showSnackbar(
                    "Purchases checked. Access opens when an active subscription is verified.",
                )
            }
        }
    }
    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbars) }) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            TextButton(onClick = onAccountClick, modifier = Modifier.padding(start = 16.dp)) {
                Text("Account & support")
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CrescentGlyph()
                Spacer(Modifier.height(20.dp))
                Text(
                    "Walk the path of Wicca",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Discover candle spells, herbal rituals, cord and knot work, and lunar practices " +
                        "inspired by historical grimoires.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                BenefitRow("A new guided Wiccan practice every day")
                Spacer(Modifier.height(14.dp))
                BenefitRow("A personal Grimoire filled with your discoveries")
                Spacer(Modifier.height(14.dp))
                BenefitRow("One subscription to explore the entire path")
                Spacer(Modifier.height(18.dp))
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.isLoadingPrice) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        when {
                            state.price == null -> "Subscription unavailable"
                            state.hasFreeTrial -> "7 days free"
                            else -> "${state.price} / month"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        when {
                            state.price == null -> "Connect to Google Play to view your subscription offer."
                            state.hasFreeTrial -> "Then ${state.price} per month. Auto-renews until canceled."
                            else -> "${state.price} per month. Trial unavailable for this Play account."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    if (state.price == null) {
                        TextButton(onClick = { viewModel.onEvent(PaywallUiEvent.RetryPriceClicked) }) {
                            Text("Retry price")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { context.findActivity()?.let(viewModel::subscribe) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoadingPrice && state.price != null,
                ) {
                    Text(if (state.hasFreeTrial) "Start My 7-Day Free Trial" else "Continue to Subscription")
                }
                OutlinedButton(
                    onClick = { viewModel.onEvent(PaywallUiEvent.RestorePurchasesClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRestoring,
                ) { Text("Restore Purchases") }
                if (state.isRestoring) CircularProgressIndicator()
                Text(
                    if (state.hasFreeTrial) {
                        "No charge during the trial. Google Play charges ${state.price} monthly after the trial " +
                            "unless canceled before it ends. Cancel anytime through Google Play."
                    } else {
                        "Google Play handles payment and automatic renewals. Cancel anytime through Google Play."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
                Row {
                    TextButton(onClick = onTermsClick) { Text("Terms") }
                    TextButton(onClick = onPrivacyClick) { Text("Privacy") }
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("✦", color = MaterialTheme.colorScheme.primary)
        Text(text, modifier = Modifier.padding(start = 12.dp))
    }
}
