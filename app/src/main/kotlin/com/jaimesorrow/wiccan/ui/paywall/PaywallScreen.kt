package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.core.util.findActivity
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanFullLogo
import com.jaimesorrow.wiccan.ui.brand.wiccanEmblemColor
import com.jaimesorrow.wiccan.ui.theme.GrimoireButton
import com.jaimesorrow.wiccan.ui.theme.GrimoireFloralBackdrop
import com.jaimesorrow.wiccan.ui.theme.MidnightGrimoireTheme
import com.jaimesorrow.wiccan.ui.theme.grimoireCard
import com.wiccan.app.R
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
    val resources = LocalResources.current
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
                is PaywallUiEffect.ShowSnackbar -> snackbars.showSnackbar(effect.error.toUserMessage(resources))
                PaywallUiEffect.PurchasesRestored -> snackbars.showSnackbar(
                    resources.getString(R.string.paywall_restore_checked),
                )
            }
        }
    }
    MidnightGrimoireTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbars) },
        ) { insets ->
            Box(Modifier.fillMaxSize()) {
                GrimoireFloralBackdrop(Modifier.fillMaxSize())
                Column(modifier = Modifier.fillMaxSize().padding(insets)) {
                    TextButton(onClick = onAccountClick, modifier = Modifier.padding(start = 16.dp)) {
                        Text(stringResource(R.string.paywall_account))
                    }
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        WiccanFullLogo(modifier = Modifier.testTag("paywall-logo"), width = 170.dp)
                        WiccanBotanicalAccent(width = 116.dp)
                        Text(
                            stringResource(R.string.paywall_headline),
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            stringResource(R.string.paywall_body),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(28.dp))
                        BenefitRow(stringResource(R.string.paywall_benefit_daily))
                        Spacer(Modifier.height(14.dp))
                        BenefitRow(stringResource(R.string.paywall_benefit_grimoire))
                        Spacer(Modifier.height(14.dp))
                        BenefitRow(stringResource(R.string.paywall_benefit_subscription))
                        Spacer(Modifier.height(18.dp))
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                            .grimoireCard().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val loadingDescription = stringResource(R.string.loading_description)
                        if (state.isLoadingPrice) {
                            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
                        } else {
                            val price = state.price
                            Text(
                                when {
                                    price == null -> stringResource(R.string.paywall_price_unavailable)
                                    state.hasFreeTrial -> stringResource(R.string.paywall_trial_free)
                                    else -> stringResource(R.string.paywall_price_month, price)
                                },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                when {
                                    price == null -> stringResource(R.string.paywall_price_connect)
                                    state.hasFreeTrial -> stringResource(R.string.paywall_price_then, price)
                                    else -> stringResource(R.string.paywall_price_no_trial, price)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                            if (price == null) {
                                TextButton(onClick = { viewModel.onEvent(PaywallUiEvent.RetryPriceClicked) }) {
                                    Text(stringResource(R.string.paywall_retry_price))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        GrimoireButton(
                            text = stringResource(
                                if (state.hasFreeTrial) R.string.paywall_start_trial else R.string.paywall_continue_subscription,
                            ),
                            onClick = { context.findActivity()?.let(viewModel::subscribe) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoadingPrice && state.price != null,
                        )
                        OutlinedButton(
                            onClick = { viewModel.onEvent(PaywallUiEvent.RestorePurchasesClicked) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isRestoring,
                        ) { Text(stringResource(R.string.paywall_restore)) }
                        if (state.isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
                        }
                        Text(
                            if (state.hasFreeTrial) {
                                stringResource(R.string.paywall_trial_disclosure, state.price.orEmpty())
                            } else {
                                stringResource(R.string.paywall_paid_disclosure)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                        Row {
                            TextButton(onClick = onTermsClick) { Text(stringResource(R.string.auth_terms)) }
                            TextButton(onClick = onPrivacyClick) { Text(stringResource(R.string.auth_privacy)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("✦", color = wiccanEmblemColor(), modifier = Modifier.clearAndSetSemantics {})
        Text(text, modifier = Modifier.padding(start = 12.dp))
    }
}
