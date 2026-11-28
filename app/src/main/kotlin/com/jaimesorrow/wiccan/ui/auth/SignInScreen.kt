package com.jaimesorrow.wiccan.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.seasonal.grimoireMoonTrack
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanFullLogo
import com.jaimesorrow.wiccan.ui.brand.wiccanEmblemColor
import com.jaimesorrow.wiccan.ui.brand.wiccanPageWash
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.theme.GrimoireButton
import com.jaimesorrow.wiccan.ui.theme.GrimoireFloralBackdrop
import com.jaimesorrow.wiccan.ui.theme.GrimoireMoonPhases
import com.jaimesorrow.wiccan.ui.theme.GrimoireOrnamentalDivider
import com.jaimesorrow.wiccan.ui.theme.GrimoireOutlineButton
import com.jaimesorrow.wiccan.ui.theme.GrimoireTextLink
import com.jaimesorrow.wiccan.ui.theme.MidnightGrimoireTheme
import com.jaimesorrow.wiccan.ui.theme.WiccanDisplayFont
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.jaimesorrow.wiccan.ui.theme.grimoireCornerFlourishes
import com.jaimesorrow.wiccan.ui.theme.grimoireFieldColors
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
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    BackHandler(enabled = state.mode == SignInMode.FORGOT_PASSWORD) {
        viewModel.onEvent(SignInUiEvent.BackToSignIn)
    }
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SignInUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.error.toUserMessage(resources))
                SignInUiEffect.PasswordResetEmailSent -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.auth_reset_snackbar),
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
    MidnightGrimoireTheme {
        val scheme = MaterialTheme.colorScheme
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = scheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { insets ->
            Box(
                modifier = Modifier.padding(insets).fillMaxSize().imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                GrimoireFloralBackdrop(Modifier.fillMaxSize())
                Column(
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxSize()
                        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WiccanFullLogo(modifier = Modifier.testTag("auth-logo"), width = 180.dp)
                    val today = java.time.LocalDate.now()
                    val signInMoon = remember(today) { grimoireMoonTrack(today) }
                    GrimoireMoonPhases(
                        active = signInMoon.trackIndex,
                        activePhaseName = signInMoon.name,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    GrimoireOrnamentalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = if (state.mode == SignInMode.FORGOT_PASSWORD) {
                            stringResource(R.string.auth_forgot_headline)
                        } else {
                            stringResource(R.string.auth_intro_headline)
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = WiccanDisplayFont),
                        color = WiccanPaletteColors.Grimoire.Gold,
                        modifier = Modifier.fillMaxWidth().semantics { heading() },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (state.mode == SignInMode.FORGOT_PASSWORD) {
                            stringResource(R.string.auth_forgot_body)
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
                            stringResource(R.string.auth_daily_line),
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WiccanPaletteColors.Grimoire.CardFill,
                        tonalElevation = 0.dp,
                        border = BorderStroke(1.dp, WiccanPaletteColors.Grimoire.Border),
                        modifier = Modifier.fillMaxWidth().grimoireCornerFlourishes(),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            when (state.mode) {
                                SignInMode.SIGN_IN, SignInMode.CREATE_ACCOUNT -> AuthFields(
                                    state,
                                    onEvent,
                                    onGoogleSignInClick,
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GrimoireTextLink(
                            text = stringResource(R.string.auth_terms),
                            onClick = { onOpenLegalDocument(LegalDocumentId.TERMS) },
                        )
                        GrimoireTextLink(
                            text = stringResource(R.string.auth_privacy),
                            onClick = { onOpenLegalDocument(LegalDocumentId.PRIVACY) },
                        )
                    }
                    WiccanBotanicalAccent(
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        width = 132.dp,
                    )
                    Text(
                        text = stringResource(R.string.auth_quiet_quote),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    if (state.isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(color = scheme.primary)
                    }
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
        stringResource(if (creating) R.string.auth_begin_path else R.string.auth_welcome_back),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    if (state.isGoogleSignInAvailable) {
        GrimoireOutlineButton(
            text = stringResource(R.string.auth_continue_google),
            onClick = onGoogleSignInClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        )
        Spacer(modifier = Modifier.height(16.dp))
        GrimoireOrnamentalDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
    }
    EmailField(state, onEvent)
    Spacer(modifier = Modifier.height(12.dp))
    PasswordField(state, onEvent)
    if (creating) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.username,
            onValueChange = { onEvent(SignInUiEvent.UsernameChanged(it)) },
            label = { Text(stringResource(R.string.auth_username)) },
            isError = state.usernameError != null,
            supportingText = state.usernameError?.let { error -> { Text(stringResource(error)) } },
            singleLine = true,
            colors = grimoireFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { onEvent(SignInUiEvent.ConfirmPasswordChanged(it)) },
            label = { Text(stringResource(R.string.auth_confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.confirmPasswordError != null,
            supportingText = state.confirmPasswordError?.let { error -> { Text(stringResource(error)) } },
            singleLine = true,
            colors = grimoireFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    GrimoireButton(
        text = stringResource(R.string.auth_enter_grimoire),
        onClick = { onEvent(SignInUiEvent.SubmitEmailAuth) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isLoading,
    )
    GrimoireTextLink(
        text = stringResource(if (creating) R.string.auth_have_account else R.string.auth_need_account),
        onClick = { onEvent(SignInUiEvent.ToggleMode) },
        enabled = !state.isLoading,
    )
    if (!creating) {
        GrimoireTextLink(
            text = stringResource(R.string.auth_forgot_password),
            onClick = { onEvent(SignInUiEvent.ForgotPasswordClicked) },
            enabled = !state.isLoading,
        )
    }
}

@Composable
private fun RecoveryFields(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit) {
    Text(stringResource(R.string.auth_reset_title), style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(18.dp))
    EmailField(state, onEvent)
    Spacer(modifier = Modifier.height(16.dp))
    GrimoireOutlineButton(
        text = stringResource(if (state.passwordResetSent) R.string.auth_resend_reset else R.string.auth_send_reset),
        onClick = { onEvent(SignInUiEvent.SubmitPasswordReset) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isLoading,
    )
    if (state.passwordResetSent) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.auth_reset_sent),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    GrimoireTextLink(
        text = stringResource(R.string.auth_back_to_sign_in),
        onClick = { onEvent(SignInUiEvent.BackToSignIn) },
        enabled = !state.isLoading,
    )
}

@Composable
private fun EmailField(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit) {
    OutlinedTextField(
        value = state.email,
        onValueChange = { onEvent(SignInUiEvent.EmailChanged(it)) },
        label = { Text(stringResource(R.string.auth_email)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        isError = state.emailError != null,
        supportingText = state.emailError?.let { error -> { Text(stringResource(error)) } },
        singleLine = true,
        colors = grimoireFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit) {
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    val showPassword = stringResource(R.string.auth_show_password)
    val hidePassword = stringResource(R.string.auth_hide_password)
    OutlinedTextField(
        value = state.password,
        onValueChange = { onEvent(SignInUiEvent.PasswordChanged(it)) },
        label = { Text(stringResource(R.string.auth_password)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(
                onClick = { onEvent(SignInUiEvent.TogglePasswordVisibility) },
                modifier = Modifier.testTag("password-visibility-toggle").semantics {
                    contentDescription = if (state.isPasswordVisible) hidePassword else showPassword
                },
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
        supportingText = state.passwordError?.let { error -> { Text(stringResource(error)) } },
        singleLine = true,
        colors = grimoireFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
