package com.wiccan.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiccan.app.core.error.toUserMessage
import com.wiccan.app.ui.today.components.CrescentGlyph
import com.wiccan.app.ui.today.components.HairlineDivider

/**
 * The only authentication screen — sign-in, account creation, and
 * password reset all live here as modes/actions rather than separate
 * screens, per spec.
 */
@Composable
fun SignInScreen(modifier: Modifier = Modifier, viewModel: SignInViewModel = hiltViewModel()) {
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
                text = "WICCAN",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(40.dp))

            if (state.isGoogleSignInAvailable) {
                Button(
                    onClick = { viewModel.onGoogleSignInClick(context) },
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
                onValueChange = { viewModel.onEvent(SignInUiEvent.EmailChanged(it)) },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(SignInUiEvent.PasswordChanged(it)) },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.onEvent(SignInUiEvent.SubmitEmailAuth) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text(if (state.mode == SignInMode.SIGN_IN) "Sign In" else "Create Account")
            }

            TextButton(onClick = { viewModel.onEvent(SignInUiEvent.ToggleMode) }) {
                Text(
                    if (state.mode == SignInMode.SIGN_IN) {
                        "Need an account? Create one"
                    } else {
                        "Have an account? Sign in"
                    },
                )
            }
            TextButton(onClick = { viewModel.onEvent(SignInUiEvent.ForgotPasswordClicked) }) {
                Text("Forgot password?")
            }

            if (state.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
