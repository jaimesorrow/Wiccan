package com.jaimesorrow.wiccan

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.domain.model.RootState
import com.jaimesorrow.wiccan.notification.isOpenTodayRequest
import com.jaimesorrow.wiccan.ui.auth.OnboardingScreen
import com.jaimesorrow.wiccan.ui.auth.RootViewModel
import com.jaimesorrow.wiccan.ui.auth.SignInScreen
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanFullLogo
import com.jaimesorrow.wiccan.ui.brand.wiccanPageWash
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentScreen
import com.jaimesorrow.wiccan.ui.legal.WiccanLegalDocuments
import com.jaimesorrow.wiccan.ui.navigation.RootRouter
import com.jaimesorrow.wiccan.ui.navigation.WiccanAppShell
import com.jaimesorrow.wiccan.ui.navigation.WiccanDeepLinks
import com.jaimesorrow.wiccan.ui.notification.NotificationSetupEffect
import com.jaimesorrow.wiccan.ui.paywall.PaywallScreen
import com.jaimesorrow.wiccan.ui.paywall.SubscriptionSetupEffect
import com.jaimesorrow.wiccan.ui.paywall.SubscriptionSetupViewModel
import com.jaimesorrow.wiccan.ui.policy.PolicyAcceptanceScreen
import com.jaimesorrow.wiccan.ui.settings.AppLanguageManager
import com.jaimesorrow.wiccan.ui.settings.SettingsScreen
import com.jaimesorrow.wiccan.ui.theme.ThemeViewModel
import com.jaimesorrow.wiccan.ui.theme.WiccanTheme
import com.wiccan.app.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Root host, resolved via [RootViewModel.rootState]: signed-out shows the
 * sign-in screen; a signed-in user owing current Terms/Privacy acceptance
 * sees the blocking [PolicyAcceptanceScreen]; only once fully ready does
 * the full navigation shell ([WiccanAppShell]) mount. No fixed timer or
 * `currentUser != null` shortcut — the AndroidX SplashScreen stays on
 * screen (per [Theme.Wiccan.Starting]) until [RootState.Loading] resolves
 * to something real.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.localizedContext(newBase))
    }

    private val rootViewModel: RootViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    /** Set by a notification tap (cold start or [onNewIntent]); cleared once the shell shows Today. */
    private var openTodayRequested by mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isOpenTodayRequest(intent)) openTodayRequested = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { rootViewModel.rootState.value == RootState.Loading }

        val startOnPaywall = intent?.data == Uri.parse(WiccanDeepLinks.PREMIUM_URI)
        // Only a fresh launch: a recreated activity has already handled its intent.
        if (savedInstanceState == null && isOpenTodayRequest(intent)) openTodayRequested = true
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            WiccanTheme(themeMode = themeMode) {
                EdgeToEdgeEffect()
                val rootState by rootViewModel.rootState.collectAsStateWithLifecycle()
                var openLegalDocument by remember { mutableStateOf<LegalDocumentId?>(null) }
                var openAccount by remember(rootState) { mutableStateOf(false) }
                if (openTodayRequested) {
                    // Close root-level overlays so Today is actually visible.
                    LaunchedEffect(Unit) { openLegalDocument = null }
                }
                val subscriptionUid = when (val state = rootState) {
                    is RootState.SignedIn -> state.uid
                    is RootState.RequiresSubscription -> state.uid
                    else -> null
                }
                val subscriptionSetupViewModel = subscriptionUid?.let { uid ->
                    key(uid) { hiltViewModel<SubscriptionSetupViewModel>(key = "subscription-setup-$uid") }
                }
                if (subscriptionSetupViewModel != null) SubscriptionSetupEffect(subscriptionSetupViewModel)
                val reconcilingSubscription by subscriptionSetupViewModel?.reconciling
                    ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

                openLegalDocument?.let { documentId ->
                    BackHandler { openLegalDocument = null }
                    LegalDocumentScreen(
                        document = WiccanLegalDocuments.get(documentId),
                        onBack = { openLegalDocument = null },
                    )
                } ?: RootRouter(
                    rootState = rootState,
                    signedIn = { uid ->
                        NotificationSetupEffect()
                        // Account deletion must finish/sign out even if the
                        // server revokes access and removes this navigation shell.
                        WiccanAppShell(
                            startOnPaywall = startOnPaywall,
                            openTodayRequested = openTodayRequested,
                            onOpenTodayHandled = { openTodayRequested = false },
                            settingsViewModel = hiltViewModel(key = "subscription-account-$uid"),
                        )
                    },
                    signedOut = { SignInScreen(onOpenLegalDocument = { openLegalDocument = it }) },
                ) { state ->
                    when (state) {
                        is RootState.RequiresSubscription -> key(subscriptionUid) {
                            if (reconcilingSubscription) {
                                LoadingIndicator(subscriptionCheck = true)
                            } else if (openAccount) {
                                SettingsScreen(
                                    accountOnly = true,
                                    onBack = { openAccount = false },
                                    onTermsClick = { openLegalDocument = LegalDocumentId.TERMS },
                                    onPrivacyClick = { openLegalDocument = LegalDocumentId.PRIVACY },
                                    onContentSafetyClick = { openLegalDocument = LegalDocumentId.CONTENT_SAFETY },
                                    onPotionSafetyClick = { openLegalDocument = LegalDocumentId.POTION_SAFETY },
                                    viewModel = hiltViewModel(key = "subscription-account-$subscriptionUid"),
                                )
                                BackHandler { openAccount = false }
                            } else {
                                PaywallScreen(
                                    onAccountClick = { openAccount = true },
                                    onTermsClick = { openLegalDocument = LegalDocumentId.TERMS },
                                    onPrivacyClick = { openLegalDocument = LegalDocumentId.PRIVACY },
                                    viewModel = hiltViewModel(key = "subscription-paywall-$subscriptionUid"),
                                )
                            }
                        }
                        RootState.RequiresEmailVerification, RootState.RequiresProfileCompletion,
                        RootState.RequiresPolicyAcceptance -> {
                            if (openAccount) {
                                SettingsScreen(
                                    accountOnly = true,
                                    onBack = { openAccount = false },
                                    onTermsClick = { openLegalDocument = LegalDocumentId.TERMS },
                                    onPrivacyClick = { openLegalDocument = LegalDocumentId.PRIVACY },
                                    onContentSafetyClick = { openLegalDocument = LegalDocumentId.CONTENT_SAFETY },
                                    onPotionSafetyClick = { openLegalDocument = LegalDocumentId.POTION_SAFETY },
                                    viewModel = hiltViewModel(key = "onboarding-account"),
                                )
                                BackHandler { openAccount = false }
                            } else if (state == RootState.RequiresPolicyAcceptance) {
                                PolicyAcceptanceScreen(
                                    onOpenLegalDocument = { openLegalDocument = it },
                                    onAccountClick = { openAccount = true },
                                )
                            } else {
                                OnboardingScreen(
                                    verification = state == RootState.RequiresEmailVerification,
                                    onOpenLegalDocument = { openLegalDocument = it },
                                    onAccountClick = { openAccount = true },
                                )
                            }
                        }
                        is RootState.AccountUnavailable -> AccountStatusError(
                            message = state.error
                                .toUserMessage(LocalResources.current),
                            onRetry = rootViewModel::retryAccountStatus,
                        )
                        RootState.Loading, RootState.InitializingProfile -> LoadingIndicator()
                        is RootState.SignedIn, RootState.SignedOut -> Unit // routed above
                    }
                }
            }
        }
    }
}

@Composable
private fun MainActivity.EdgeToEdgeEffect(colorScheme: ColorScheme = MaterialTheme.colorScheme) {
    val background = colorScheme.background.toArgb()
    val usesLightBars = colorScheme.background.luminance() > 0.5f
    SideEffect {
        val style = if (usesLightBars) {
            SystemBarStyle.light(background, background)
        } else {
            SystemBarStyle.dark(background)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

@Composable
internal fun LoadingIndicator(subscriptionCheck: Boolean = false) {
    // Paint the palette background: the window behind is the fixed dark splash
    // colour, which would hide the Light/Ivory logo.
    Box(
        modifier = Modifier.fillMaxSize().wiccanPageWash(),
        contentAlignment = Alignment.Center,
    ) {
        val loadingDescription = stringResource(R.string.loading_description)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WiccanFullLogo(modifier = Modifier.testTag("loading-logo"), width = 160.dp)
            WiccanBotanicalAccent(modifier = Modifier.padding(vertical = 7.dp), width = 116.dp)
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = loadingDescription },
                color = MaterialTheme.colorScheme.primary,
            )
            if (subscriptionCheck) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.subscription_check_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.subscription_check_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
internal fun AccountStatusError(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().wiccanPageWash(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            WiccanBotanicalAccent(modifier = Modifier.padding(bottom = 12.dp), width = 116.dp)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.today_retry))
            }
        }
    }
}
