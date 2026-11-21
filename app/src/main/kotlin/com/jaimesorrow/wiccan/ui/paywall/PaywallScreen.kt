package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.core.util.findActivity
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import com.jaimesorrow.wiccan.ui.brand.WiccanFullLogo
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
    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbars) }) { insets ->
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
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.paywall_headline),
                    style = MaterialTheme.typography.headlineMedium,
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
                Text(
                    stringResource(R.string.paywall_inside_heading),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                BenefitRow(stringResource(R.string.paywall_inside_grimoire))
                Spacer(Modifier.height(12.dp))
                BenefitRow(stringResource(R.string.paywall_inside_daily))
                if (SEASONAL_RITES_LIVE) {
                    Spacer(Modifier.height(12.dp))
                    BenefitRow(stringResource(R.string.paywall_inside_seasonal))
                }
                Spacer(Modifier.height(12.dp))
                BenefitRow(stringResource(R.string.paywall_inside_sources))
                Spacer(Modifier.height(20.dp))
                if (state.hasYearlyPlan) {
                    PlanChoice(state = state, onSelect = { viewModel.onEvent(PaywallUiEvent.PlanSelected(it)) })
                    Spacer(Modifier.height(12.dp))
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val loadingDescription = stringResource(R.string.loading_description)
                if (state.isLoadingPrice) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
                } else if (state.hasYearlyPlan) {
                    SelectedPlanTerms(state)
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
                Button(
                    onClick = { context.findActivity()?.let(viewModel::subscribe) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoadingPrice && state.price != null,
                ) {
                    Text(
                        stringResource(
                            if (state.selectedHasFreeTrial) R.string.paywall_start_trial else R.string.paywall_continue_subscription,
                        ),
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.onEvent(PaywallUiEvent.RestorePurchasesClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRestoring,
                ) { Text(stringResource(R.string.paywall_restore)) }
                if (state.isRestoring) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
                }
                Text(
                    when {
                        !state.selectedHasFreeTrial -> stringResource(R.string.paywall_paid_disclosure)
                        state.hasYearlyPlan && state.selectedPlan == SubscriptionPlan.YEARLY ->
                            stringResource(R.string.paywall_trial_disclosure_year, state.yearlyPrice.orEmpty())
                        else -> stringResource(R.string.paywall_trial_disclosure, state.price.orEmpty())
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

/**
 * The seasonal benefit is shown only once moon-phase and sabbat content is
 * live (v1.1 item 3), so the paywall never promises something the app lacks.
 */
internal const val SEASONAL_RITES_LIVE = false

/** Plain renewal terms for the plan the checkout button will buy. */
@Composable
private fun SelectedPlanTerms(state: PaywallUiState) {
    val yearly = state.selectedPlan == SubscriptionPlan.YEARLY
    val price = state.selectedPrice.orEmpty()
    Text(
        when {
            state.selectedHasFreeTrial -> stringResource(R.string.paywall_trial_free)
            yearly -> stringResource(R.string.paywall_plan_yearly_price, price)
            else -> stringResource(R.string.paywall_price_month, price)
        },
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        when {
            yearly && state.selectedHasFreeTrial -> stringResource(R.string.paywall_price_then_year, price)
            yearly -> stringResource(R.string.paywall_price_no_trial_year, price)
            state.selectedHasFreeTrial -> stringResource(R.string.paywall_price_then, price)
            else -> stringResource(R.string.paywall_price_no_trial, price)
        },
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}

/** Yearly first as the best value, monthly as the secondary choice. Both are standalone plans. */
@Composable
private fun PlanChoice(state: PaywallUiState, onSelect: (SubscriptionPlan) -> Unit) {
    Text(
        stringResource(R.string.paywall_choose_plan),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PlanCard(
            title = stringResource(R.string.paywall_plan_yearly),
            badge = stringResource(R.string.paywall_plan_best_value),
            price = stringResource(R.string.paywall_plan_yearly_price, state.yearlyPrice.orEmpty()),
            details = listOfNotNull(
                state.yearlyMonthlyEquivalent?.let { stringResource(R.string.paywall_plan_yearly_equivalent, it) },
                state.yearlySavingsPercent?.let { stringResource(R.string.paywall_plan_yearly_savings, it) },
                if (state.yearlyHasFreeTrial) stringResource(R.string.paywall_plan_trial_badge) else null,
            ),
            selected = state.selectedPlan == SubscriptionPlan.YEARLY,
            onClick = { onSelect(SubscriptionPlan.YEARLY) },
            tag = "paywall-plan-yearly",
        )
        PlanCard(
            title = stringResource(R.string.paywall_plan_monthly),
            badge = null,
            price = stringResource(R.string.paywall_plan_monthly_price, state.price.orEmpty()),
            details = listOfNotNull(
                stringResource(R.string.paywall_plan_monthly_detail),
                if (state.hasFreeTrial) stringResource(R.string.paywall_plan_trial_badge) else null,
            ),
            selected = state.selectedPlan == SubscriptionPlan.MONTHLY,
            onClick = { onSelect(SubscriptionPlan.MONTHLY) },
            tag = "paywall-plan-monthly",
        )
    }
}

@Composable
private fun PlanCard(
    title: String,
    badge: String?,
    price: String,
    details: List<String>,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    val selectedDescription = stringResource(R.string.paywall_plan_selected_description)
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().testTag(tag)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { if (selected) stateDescription = selectedDescription },
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (badge != null) {
                        Text(
                            badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Text(price, style = MaterialTheme.typography.bodyLarge)
                details.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
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
