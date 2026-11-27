package com.jaimesorrow.wiccan.ui.paywall

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
        PaywallContent(
            state = state,
            insets = insets,
            onAccountClick = onAccountClick,
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick,
            onEvent = viewModel::onEvent,
            onSubscribe = { context.findActivity()?.let(viewModel::subscribe) },
            onManageSubscription = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_SUBSCRIPTIONS_URL)))
            },
        )
    }
}

/**
 * One scrolling column with a sticky checkout footer. The plan cards come
 * right after the headline so both are on screen without scrolling on a
 * typical phone; the footer always names the selected plan, its price and
 * period, and that it auto-renews, next to the checkout button.
 */
@Composable
internal fun PaywallContent(
    state: PaywallUiState,
    insets: PaddingValues,
    onAccountClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onEvent: (PaywallUiEvent) -> Unit,
    onSubscribe: () -> Unit,
    onManageSubscription: () -> Unit = {},
) {
    val loadingDescription = stringResource(R.string.loading_description)
    Column(modifier = Modifier.fillMaxSize().padding(insets)) {
        TextButton(onClick = onAccountClick, modifier = Modifier.padding(start = 16.dp)) {
            Text(stringResource(R.string.paywall_account))
        }
        Column(
            modifier = Modifier.weight(1f).testTag("paywall-scroll").verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WiccanFullLogo(modifier = Modifier.testTag("paywall-logo"), width = 120.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.paywall_headline),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            if (state.subscriptionOnHold) {
                OutlinedCard(modifier = Modifier.fillMaxWidth().testTag("payment-on-hold")) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.paywall_payment_failed_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(stringResource(R.string.paywall_payment_failed_body))
                        TextButton(onClick = onManageSubscription) {
                            Text(stringResource(R.string.paywall_update_payment))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (state.hasYearlyPlan) {
                PlanChoice(state = state, onSelect = { onEvent(PaywallUiEvent.PlanSelected(it)) })
                Spacer(Modifier.height(12.dp))
            }
            Text(
                when {
                    !state.selectedHasFreeTrial -> stringResource(R.string.paywall_paid_disclosure)
                    state.hasYearlyPlan && state.selectedPlan == SubscriptionPlan.YEARLY ->
                        stringResource(
                            R.string.paywall_trial_disclosure_year,
                            state.selectedFreeTrialDays ?: 0,
                            state.yearlyPrice.orEmpty(),
                        )
                    else -> stringResource(
                        R.string.paywall_trial_disclosure,
                        state.selectedFreeTrialDays ?: 0,
                        state.price.orEmpty(),
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
            val winBackPrice = if (state.selectedPlan == SubscriptionPlan.YEARLY) {
                state.yearlyWinBackPrice
            } else {
                state.monthlyWinBackPrice
            }
            if (winBackPrice != null) {
                Text(
                    stringResource(R.string.paywall_winback_offer, winBackPrice),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("winback-offer"),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.paywall_restore),
                modifier = Modifier.clickable(enabled = !state.isRestoring) {
                    onEvent(PaywallUiEvent.RestorePurchasesClicked)
                }.padding(12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.support_contact_message),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
            if (state.isRestoring) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.paywall_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
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
            Row {
                TextButton(onClick = onTermsClick) { Text(stringResource(R.string.auth_terms)) }
                TextButton(onClick = onPrivacyClick) { Text(stringResource(R.string.auth_privacy)) }
            }
        }
        // Sticky checkout: the selected plan's terms sit directly above the button.
        Column(
            modifier = Modifier.fillMaxWidth().testTag("paywall-footer")
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isLoadingPrice && state.price == null) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
            } else if (state.hasYearlyPlan) {
                SelectedPlanTerms(state)
            } else {
                val price = state.price
                val winBackPrice = state.monthlyWinBackPrice
                Text(
                    when {
                        price == null -> stringResource(R.string.paywall_price_unavailable)
                        state.hasFreeTrial -> stringResource(R.string.paywall_trial_free, state.freeTrialDays ?: 0)
                        winBackPrice != null -> stringResource(
                            R.string.paywall_selected_monthly_winback,
                            winBackPrice,
                            price,
                        )
                        else -> stringResource(R.string.paywall_price_month, price)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    when {
                        price == null -> stringResource(R.string.paywall_price_connect)
                        state.hasFreeTrial -> stringResource(R.string.paywall_price_then, price)
                        winBackPrice != null -> stringResource(R.string.paywall_selected_renews)
                        else -> stringResource(R.string.paywall_price_no_trial, price)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
                if (price == null) {
                    TextButton(onClick = { onEvent(PaywallUiEvent.RetryPriceClicked) }) {
                        Text(stringResource(R.string.paywall_retry_price))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth().testTag("paywall-subscribe"),
                enabled = !state.isLoadingPrice && state.price != null,
            ) {
                Text(
                    if (state.selectedHasFreeTrial) {
                        stringResource(R.string.paywall_start_trial, state.selectedFreeTrialDays ?: 0)
                    } else {
                        stringResource(R.string.paywall_continue_subscription)
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private const val PLAY_SUBSCRIPTIONS_URL =
    "https://play.google.com/store/account/subscriptions?package=com.jaimesorrow.wiccan"

/**
 * The seasonal benefit is shown only once moon-phase and sabbat content is
 * live (v1.1 item 3), so the paywall never promises something the app lacks.
 */
internal const val SEASONAL_RITES_LIVE = false

/**
 * Plain terms for the plan the checkout button will buy, always naming the
 * plan: e.g. "Yearly: 14 days free, then $39.99/year" and "Auto-renews until
 * canceled." Prices are Play's formatted prices.
 */
@Composable
private fun SelectedPlanTerms(state: PaywallUiState) {
    val yearly = state.selectedPlan == SubscriptionPlan.YEARLY
    val price = state.selectedPrice.orEmpty()
    val winBackPrice = if (yearly) state.yearlyWinBackPrice else state.monthlyWinBackPrice
    Text(
        when {
            yearly && winBackPrice != null && state.yearlyWinBackBillingPeriod == "P1M" ->
                stringResource(R.string.paywall_selected_yearly_winback_month, winBackPrice, price)
            yearly && winBackPrice != null -> stringResource(R.string.paywall_selected_yearly_winback, winBackPrice, price)
            winBackPrice != null -> stringResource(R.string.paywall_selected_monthly_winback, winBackPrice, price)
            yearly && state.selectedHasFreeTrial -> stringResource(
                R.string.paywall_selected_yearly_trial,
                state.selectedFreeTrialDays ?: 0,
                price,
            )
            yearly -> stringResource(R.string.paywall_selected_yearly, price)
            state.selectedHasFreeTrial -> stringResource(
                R.string.paywall_selected_monthly_trial,
                state.selectedFreeTrialDays ?: 0,
                price,
            )
            else -> stringResource(R.string.paywall_selected_monthly, price)
        },
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("paywall-selected-terms"),
    )
    Text(
        when {
            winBackPrice != null -> stringResource(R.string.paywall_selected_renews)
            yearly && state.selectedHasFreeTrial -> stringResource(R.string.paywall_selected_renews)
            yearly -> stringResource(R.string.paywall_price_no_trial_year, price)
            state.selectedHasFreeTrial -> stringResource(R.string.paywall_selected_renews)
            else -> stringResource(R.string.paywall_price_no_trial_month_plain, price)
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
    Spacer(Modifier.height(8.dp))
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlanCard(
            title = stringResource(R.string.paywall_plan_yearly),
            badge = state.yearlySavingsPercent?.takeIf { it > 0 }?.let {
                stringResource(R.string.paywall_plan_best_value)
            },
            price = stringResource(R.string.paywall_plan_yearly_price, state.yearlyPrice.orEmpty()),
            details = listOfNotNull(
                state.yearlyMonthlyEquivalent?.let { stringResource(R.string.paywall_plan_yearly_equivalent, it) },
                state.yearlySavingsPercent?.let { stringResource(R.string.paywall_plan_yearly_savings, it) },
                if (state.yearlyHasFreeTrial) {
                    stringResource(R.string.paywall_plan_trial_badge, state.yearlyFreeTrialDays ?: 0)
                } else {
                    null
                },
            ).joinToString(" · ").takeIf { it.isNotEmpty() },
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
                if (state.hasFreeTrial) {
                    stringResource(R.string.paywall_plan_trial_badge, state.freeTrialDays ?: 0)
                } else {
                    null
                },
            ).joinToString(" · "),
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
    details: String?,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().testTag(tag)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                // Wraps the badge under the title for long translations or large text.
                FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 8.dp))
                    if (badge != null) {
                        Text(
                            badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(price, style = MaterialTheme.typography.bodyLarge)
                if (details != null) Text(details, style = MaterialTheme.typography.bodySmall)
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
