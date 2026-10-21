package com.jaimesorrow.wiccan.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.today.components.CrescentGlyph
import com.jaimesorrow.wiccan.ui.today.components.HairlineDivider
import com.wiccan.app.R

/**
 * The only authentication screen — sign-in, account creation, and
 * password reset all live here as modes/actions rather than separate
 * screens, per spec.
 */
@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    onOpenLegalDocument: (LegalDocumentId) -> Unit = {},
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SignInUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.error.toUserMessage())
                SignInUiEffect.PasswordResetEmailSent ->
                    snackbarHostState.showSnackbar("Password reset email sent.")
            }
        }
    }

    SignInScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onGoogleSignInClick = { viewModel.onGoogleSignInClick(context) },
        onOpenLegalDocument = onOpenLegalDocument,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
internal fun SignInScreenContent(
    state: SignInUiState,
    onEvent: (SignInUiEvent) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onOpenLegalDocument: (LegalDocumentId) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        Box(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CrescentGlyph(modifier = Modifier.testTag("auth-crescent"))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "WICCAN",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.auth_intro_headline),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.auth_intro_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(32.dp))

                if (state.isGoogleSignInAvailable) {
                    Button(
                        onClick = onGoogleSignInClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading,
                    ) {
                        Text("Continue with Google")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HairlineDivider(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(24.dp))
                }

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onEvent(SignInUiEvent.EmailChanged(it)) },
                    label = { Text("Email") },
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { error -> { Text(error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onEvent(SignInUiEvent.PasswordChanged(it)) },
                    label = { Text("Password") },
                    isError = state.passwordError != null,
                    supportingText = state.passwordError?.let { error -> { Text(error) } },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onEvent(SignInUiEvent.SubmitEmailAuth) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                ) {
                    Text(if (state.mode == SignInMode.SIGN_IN) "Sign In" else "Create Account")
                }

                TextButton(onClick = { onEvent(SignInUiEvent.ToggleMode) }) {
                    Text(
                        if (state.mode == SignInMode.SIGN_IN) {
                            "Need an account? Create one"
                        } else {
                            "Have an account? Sign in"
                        },
                    )
                }
                TextButton(onClick = { onEvent(SignInUiEvent.ForgotPasswordClicked) }) {
                    Text("Forgot password?")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auth_subscription_requirement),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.TERMS) }) {
                        Text("Terms")
                    }
                    TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.PRIVACY) }) {
                        Text("Privacy")
                    }
                }

                if (state.isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
