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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.core.util.findActivity
import com.jaimesorrow.wiccan.ui.today.components.CrescentGlyph

/**
 * The single premium upsell surface — two real benefits, the runtime Play
 * price, subscribe, and restore. No pricing ladder, no comparison table:
 * one subscription, one screen.
 */
@Composable
fun PaywallScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PaywallUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.error.toUserMessage())
                PaywallUiEffect.PurchasesRestored -> snackbarHostState.showSnackbar("Purchases restored.")
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        Column(modifier = Modifier.padding(insets).fillMaxSize()) {
            TextButton(onClick = onBack, modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                Text("← Back")
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
                    text = "WICCAN PREMIUM",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(32.dp))

                BenefitRow("Access to every premium spell, potion, and lesson — not just the free daily pick")
                Spacer(modifier = Modifier.height(16.dp))
                BenefitRow("Home-screen widget access, so today's content is there without opening the app")

                Spacer(modifier = Modifier.height(40.dp))

                if (state.isLoadingPrice) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(
                        text = state.price ?: "Price unavailable",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (!state.isLoadingPrice) {
                    Text(
                        text = "${state.price ?: "This price"} per month. Auto-renews until canceled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { context.findActivity()?.let(viewModel::subscribe) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoadingPrice && state.price != null,
                ) {
                    Text("Subscribe")
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
                    text = "Cancel anytime through Google Play. You can keep using Wiccan's free daily content either way.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
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
