package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

/**
 * Required after onboarding. A purchase callback alone never dismisses
 * this screen; the root host waits for server-verified subscription access.
 */
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
    val snackbarHostState = remember { SnackbarHostState() }
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
                is PaywallUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.error.toUserMessage())
                PaywallUiEffect.PurchasesRestored -> snackbarHostState.showSnackbar(
                    "Purchases checked. Access opens when an active subscription is verified."
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        Column(modifier = Modifier.padding(insets).fillMaxSize()) {
            TextButton(onClick = onAccountClick, modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                Text("Account & support")
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CrescentGlyph()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Walk the path of Wicca",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Explore candle spells, herbal rituals, cord and knot work, and lunar practices inspired by historical grimoires.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(28.dp))

                BenefitRow("A guided Wiccan practice to discover each day")
                Spacer(modifier = Modifier.height(16.dp))
                BenefitRow("Build your personal Grimoire and revisit everything you learn")
                Spacer(modifier = Modifier.height(16.dp))
                BenefitRow("One subscription. Your journey, at your own pace.")

                Spacer(modifier = Modifier.height(40.dp))

                if (state.isLoadingPrice) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(
                        text = when {
                            state.price == null -> "Subscription unavailable"
                            state.hasFreeTrial -> "7 days free"
                            else -> "${state.price} / month"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (!state.isLoadingPrice && state.price != null) {
                    Text(
                        text = if (state.hasFreeTrial) {
                            "Then ${state.price} per month. Auto-renews until canceled."
                        } else {
                            "No free trial is available for this Google Play account. Auto-renews until canceled."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!state.isLoadingPrice && state.price == null) {
                    TextButton(onClick = { viewModel.onEvent(PaywallUiEvent.RetryPriceClicked) }) {
                        Text("Retry price")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { context.findActivity()?.let(viewModel::subscribe) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoadingPrice && state.price != null,
                ) {
                    Text(if (state.hasFreeTrial) "Start My 7-Day Free Trial" else "Continue to Subscription")
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.onEvent(PaywallUiEvent.RestorePurchasesClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRestoring,
                ) {
                    Text("Restore Purchases")
                }

                if (state.isRestoring) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (state.hasFreeTrial) {
                        "No charge during the 7-day trial. Google Play automatically charges ${state.price} per month " +
                            "after the trial unless you cancel before it ends. Cancel anytime in Google Play → " +
                            "Payments & subscriptions → Subscriptions. Trial eligibility is determined by Google Play."
                    } else if (state.price != null) {
                        "Google Play charges ${state.price} when you confirm and every month thereafter until canceled. " +
                            "Cancel anytime in Google Play → Payments & subscriptions → Subscriptions."
                    } else {
                        "Connect to Google Play to load your subscription offer, or restore an existing subscription."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onTermsClick) { Text("Terms") }
                    TextButton(onClick = onPrivacyClick) { Text("Privacy") }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "✦",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
