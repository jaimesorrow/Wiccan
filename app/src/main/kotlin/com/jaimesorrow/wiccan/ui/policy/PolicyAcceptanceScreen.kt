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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.today.components.CrescentGlyph

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

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PolicyAcceptanceUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.error.toUserMessage())
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CrescentGlyph()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Before you continue",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please review each document and confirm every statement below to keep using Wiccan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.TERMS) }) {
                    Text("Terms of Service")
                }
                TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.PRIVACY) }) {
                    Text("Privacy Policy")
                }
            }
            TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.CONTENT_SAFETY) }) {
                Text("Content & Safety")
            }
            Spacer(modifier = Modifier.height(4.dp))

            PolicyAgreement.entries.forEach { agreement ->
                AgreementRow(
                    text = agreement.label(),
                    checked = agreement in state.agreed,
                    onCheckedChange = { viewModel.onEvent(PolicyAcceptanceUiEvent.AgreementChanged(agreement, it)) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canContinue,
            ) {
                Text("Continue")
            }

            TextButton(onClick = onAccountClick) { Text("Account & support / Delete Account") }

            if (state.isSubmitting) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Statement the user must check for each required agreement. */
internal fun PolicyAgreement.label(): String = when (this) {
    PolicyAgreement.ADULT -> "I am 18 years of age or older."
    PolicyAgreement.TERMS -> "I have read and agree to the Terms of Service."
    PolicyAgreement.PRIVACY -> "I have read and acknowledge the Privacy Policy."
    PolicyAgreement.NO_GUARANTEE ->
        "I understand that Wiccan does not promise or guarantee any result from any spell, potion, ritual, " +
            "ceremony, or other practice, and I have read the Content & Safety guidance."
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
