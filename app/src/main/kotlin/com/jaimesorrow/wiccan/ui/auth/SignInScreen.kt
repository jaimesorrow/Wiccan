package com.jaimesorrow.wiccan.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.today.components.CrescentGlyph
import com.jaimesorrow.wiccan.ui.today.components.HairlineDivider
import com.wiccan.app.R

/** Independent sign-in, account creation and password recovery views in a shared themed host. */
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
                SignInUiEffect.PasswordResetEmailSent -> snackbarHostState.showSnackbar(
                    "If an account uses this email, a reset link will arrive shortly.",
                )
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
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        Box(
            modifier = Modifier.padding(insets).fillMaxSize()
                .background(Brush.verticalGradient(listOf(scheme.background, scheme.surface, scheme.background)))
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxSize()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CrescentGlyph(modifier = Modifier.testTag("auth-crescent"))
                Spacer(modifier = Modifier.height(10.dp))
                Text("W I C C A N", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (state.mode == SignInMode.FORGOT_PASSWORD) "Find your way back." else
                        stringResource(R.string.auth_intro_headline),
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.onBackground,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (state.mode == SignInMode.FORGOT_PASSWORD) {
                        "Enter your email address. We'll send a secure link to reset your password."
                    } else {
                        stringResource(R.string.auth_intro_body)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.mode != SignInMode.FORGOT_PASSWORD) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "A new practice each day. A Grimoire to call your own.",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = scheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        when (state.mode) {
                            SignInMode.SIGN_IN, SignInMode.CREATE_ACCOUNT -> AuthFields(
                                state, onEvent, onGoogleSignInClick,
                            )
                            SignInMode.FORGOT_PASSWORD -> RecoveryFields(state, onEvent)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.auth_subscription_requirement),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Row {
                    TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.TERMS) }) { Text("Terms") }
                    TextButton(onClick = { onOpenLegalDocument(LegalDocumentId.PRIVACY) }) { Text("Privacy") }
                }
                if (state.isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(color = scheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AuthFields(
    state: SignInUiState,
    onEvent: (SignInUiEvent) -> Unit,
    onGoogleSignInClick: () -> Unit,
) {
    val creating = state.mode == SignInMode.CREATE_ACCOUNT
    Text(
        if (creating) "Begin your Wiccan journey" else "Welcome back to your path",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    if (state.isGoogleSignInAvailable) {
        Button(onClick = onGoogleSignInClick, modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading) {
            Text("Continue with Google")
        }
        Spacer(modifier = Modifier.height(16.dp))
        HairlineDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
    }
    EmailField(state, onEvent)
    Spacer(modifier = Modifier.height(12.dp))
    PasswordField(state, onEvent)
    if (creating) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { onEvent(SignInUiEvent.ConfirmPasswordChanged(it)) },
            label = { Text("Confirm password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.confirmPasswordError != null,
            supportingText = state.confirmPasswordError?.let { error -> { Text(error) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { onEvent(SignInUiEvent.SubmitEmailAuth) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isLoading,
    ) {
        Text(if (creating) "Create Account" else "Sign In")
    }
    TextButton(onClick = { onEvent(SignInUiEvent.ToggleMode) }) {
        Text(if (creating) "Have an account? Sign in" else "Need an account? Create one")
    }
    if (!creating) {
        TextButton(onClick = { onEvent(SignInUiEvent.ForgotPasswordClicked) }) { Text("Forgot password?") }
    }
}

@Composable
private fun RecoveryFields(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit) {
    Text("Reset your password", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(18.dp))
    EmailField(state, onEvent)
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { onEvent(SignInUiEvent.SubmitPasswordReset) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isLoading,
    ) { Text(if (state.passwordResetSent) "Resend reset link" else "Send reset link") }
    if (state.passwordResetSent) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Check your inbox and spam folder. If that email has an account, a reset link has been sent.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    TextButton(onClick = { onEvent(SignInUiEvent.BackToSignIn) }) { Text("Back to Sign In") }
}

@Composable
private fun EmailField(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit) {
    OutlinedTextField(
        value = state.email,
        onValueChange = { onEvent(SignInUiEvent.EmailChanged(it)) },
        label = { Text("Email") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        isError = state.emailError != null,
        supportingText = state.emailError?.let { error -> { Text(error) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit) {
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    OutlinedTextField(
        value = state.password,
        onValueChange = { onEvent(SignInUiEvent.PasswordChanged(it)) },
        label = { Text("Password") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(
                onClick = { onEvent(SignInUiEvent.TogglePasswordVisibility) },
                modifier = Modifier.testTag("password-visibility-toggle"),
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawOval(
                        color = iconColor,
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * .08f, size.height * .25f),
                        size = androidx.compose.ui.geometry.Size(size.width * .84f, size.height * .50f),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                    drawCircle(color = iconColor, radius = size.minDimension * .13f)
                    if (!state.isPasswordVisible) {
                        drawLine(
                            color = iconColor,
                            start = androidx.compose.ui.geometry.Offset(0f, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }
            }
        },
        isError = state.passwordError != null,
        supportingText = state.passwordError?.let { error -> { Text(error) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
