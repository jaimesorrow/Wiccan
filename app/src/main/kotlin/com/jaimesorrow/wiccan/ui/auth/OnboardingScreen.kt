package com.jaimesorrow.wiccan.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.today.components.CrescentGlyph

@Composable
fun OnboardingScreen(
    verification: Boolean,
    onOpenLegalDocument: (LegalDocumentId) -> Unit = {},
    onAccountClick: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier.padding(insets).fillMaxSize().imePadding()
                .verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CrescentGlyph()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                if (verification) "Confirm your path." else "Make this journey yours.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (verification) {
                    "Check your inbox for a verification message from Wiccan. Open the link, then return here to continue."
                } else {
                    "Add your name to complete your Wiccan account. No billing address or card details are needed here."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (verification) {
                Button(onClick = viewModel::checkVerification, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("I've Verified My Email")
                }
                TextButton(onClick = viewModel::resendVerification, enabled = !state.busy) {
                    Text("Resend Verification Email")
                }
            } else {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = viewModel::setFirstName,
                    label = { Text("First name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = viewModel::setLastName,
                    label = { Text("Last name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = viewModel::completeProfile, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue")
                }
            }
            if (state.busy) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator()
            }
            state.notice?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(18.dp))
            TextButton(onClick = viewModel::signOut, enabled = !state.busy) { Text("Sign Out") }
            TextButton(onClick = onAccountClick) { Text("Account & support / Delete Account") }
            TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.PRIVACY) }) { Text("Privacy Policy") }
        }
    }
}
