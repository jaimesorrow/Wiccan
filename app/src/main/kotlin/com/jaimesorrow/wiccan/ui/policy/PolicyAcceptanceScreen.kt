package com.jaimesorrow.wiccan.ui.policy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.today.components.CrescentGlyph

/**
 * Blocking agreement gate shown whenever the signed-in user hasn't yet
 * accepted the current Terms of Service/Privacy Policy. Continue is
 * disabled until the checkbox is checked; on success, [PolicyAcceptanceViewModel]
 * does no navigation of its own - the root observer moves on once the
 * server records the acceptance.
 */
@Composable
fun PolicyAcceptanceScreen(
    modifier: Modifier = Modifier,
    onOpenLegalDocument: (LegalDocumentId) -> Unit = {},
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
                .padding(horizontal = 32.dp),
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
                text = "Please review and agree to our policies to keep using Wiccan.",
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

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = state.isChecked,
                    onCheckedChange = { viewModel.onEvent(PolicyAcceptanceUiEvent.CheckedChanged(it)) },
                )
                Text(
                    text = "I agree to the Terms of Service and acknowledge the Privacy Policy and safety guidance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isChecked && !state.isSubmitting,
            ) {
                Text("Continue")
            }

            if (state.isSubmitting) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
