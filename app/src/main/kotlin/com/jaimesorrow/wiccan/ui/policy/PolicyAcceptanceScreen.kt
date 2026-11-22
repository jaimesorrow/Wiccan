package com.jaimesorrow.wiccan.ui.policy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanEmblem
import com.jaimesorrow.wiccan.ui.brand.wiccanPageWash
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.wiccan.app.R

/**
 * Blocking agreement gate shown whenever the signed-in user hasn't yet
 * accepted the current Terms of Service/Privacy Policy. Continue is
 * disabled until every [PolicyAgreement] is checked (18+, Terms, Privacy,
 * and the no-guaranteed-results/safety acknowledgement); on success, [PolicyAcceptanceViewModel]
 * does no navigation of its own - the root observer moves on once the
 * server records the acceptance.
 */
@Composable
fun PolicyAcceptanceScreen(
    modifier: Modifier = Modifier,
    onOpenLegalDocument: (LegalDocumentId) -> Unit = {},
    onAccountClick: () -> Unit = {},
    viewModel: PolicyAcceptanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val loadingDescription = stringResource(R.string.loading_description)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PolicyAcceptanceUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(
                    effect.error.toUserMessage(resources),
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize()
                .wiccanPageWash()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WiccanEmblem(
                modifier = Modifier.testTag("policy-logo"),
                size = 56.dp,
                contentDescription = stringResource(R.string.policy_logo_description),
            )
            WiccanBotanicalAccent(width = 108.dp)
            Text(
                text = stringResource(R.string.policy_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.policy_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.TERMS) }) {
                    Text(stringResource(R.string.policy_terms))
                }
                TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.PRIVACY) }) {
                    Text(stringResource(R.string.policy_privacy))
                }
            }
            TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.CONTENT_SAFETY) }) {
                Text(stringResource(R.string.policy_safety))
            }
            Spacer(modifier = Modifier.height(4.dp))

            PolicyAgreement.entries.forEach { agreement ->
                AgreementRow(
                    text = stringResource(agreement.labelRes()),
                    checked = agreement in state.agreed,
                    onCheckedChange = { viewModel.onEvent(PolicyAcceptanceUiEvent.AgreementChanged(agreement, it)) },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.policy_optional),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            AgreementRow(
                text = stringResource(R.string.policy_share_analytics),
                checked = state.shareAnalytics,
                onCheckedChange = { viewModel.onEvent(PolicyAcceptanceUiEvent.ShareAnalyticsChanged(it)) },
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canContinue,
            ) {
                Text(stringResource(R.string.policy_continue))
            }

            TextButton(onClick = onAccountClick) { Text(stringResource(R.string.onboarding_account)) }

            if (state.isSubmitting) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Statement the user must check for each required agreement. Wording stays plain. */
internal fun PolicyAgreement.labelRes(): Int = when (this) {
    PolicyAgreement.ADULT -> R.string.policy_adult
    PolicyAgreement.TERMS -> R.string.policy_agree_terms
    PolicyAgreement.PRIVACY -> R.string.policy_ack_privacy
    PolicyAgreement.NO_GUARANTEE -> R.string.policy_no_guarantee
}

@Composable
private fun AgreementRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        )
    }
}
