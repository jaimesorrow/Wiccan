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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanFullLogo
import com.jaimesorrow.wiccan.ui.brand.wiccanPageWash
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.wiccan.app.R

@Composable
fun OnboardingScreen(
    verification: Boolean,
    onOpenLegalDocument: (LegalDocumentId) -> Unit = {},
    onAccountClick: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val step = if (verification) OnboardingStep.VERIFY_EMAIL else OnboardingStep.PROFILE
    LaunchedEffect(step) { viewModel.onStepShown(step) }
    val loadingDescription = stringResource(R.string.loading_description)
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier.padding(insets).fillMaxSize().wiccanPageWash().imePadding()
                .verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WiccanFullLogo(modifier = Modifier.testTag("onboarding-logo"), width = 150.dp)
            WiccanBotanicalAccent(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                stringResource(if (verification) R.string.onboarding_verify_title else R.string.onboarding_name_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(if (verification) R.string.onboarding_verify_body else R.string.onboarding_name_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (verification) {
                Button(onClick = viewModel::checkVerification, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_verified_button))
                }
                TextButton(onClick = viewModel::resendVerification, enabled = !state.busy) {
                    Text(stringResource(R.string.onboarding_resend_button))
                }
            } else {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = viewModel::setFirstName,
                    label = { Text(stringResource(R.string.onboarding_first_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = viewModel::setLastName,
                    label = { Text(stringResource(R.string.onboarding_last_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = viewModel::completeProfile, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_continue))
                }
            }
            if (state.busy) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
            }
            state.noticeFor(step)?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(message),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("onboarding-notice"),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            TextButton(onClick = viewModel::signOut, enabled = !state.busy) {
                Text(stringResource(R.string.onboarding_sign_out))
            }
            TextButton(onClick = onAccountClick) { Text(stringResource(R.string.onboarding_account)) }
            TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.PRIVACY) }) {
                Text(stringResource(R.string.onboarding_privacy))
            }
        }
    }
}
