package com.jaimesorrow.wiccan

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.domain.model.RootState
import com.jaimesorrow.wiccan.ui.auth.RootViewModel
import com.jaimesorrow.wiccan.ui.auth.SignInScreen
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentScreen
import com.jaimesorrow.wiccan.ui.legal.WiccanLegalDocuments
import com.jaimesorrow.wiccan.ui.navigation.WiccanAppShell
import com.jaimesorrow.wiccan.ui.navigation.WiccanDeepLinks
import com.jaimesorrow.wiccan.ui.notification.NotificationSetupEffect
import com.jaimesorrow.wiccan.ui.paywall.PaywallScreen
import com.jaimesorrow.wiccan.ui.paywall.SubscriptionSetupEffect
import com.jaimesorrow.wiccan.ui.policy.PolicyAcceptanceScreen
import com.jaimesorrow.wiccan.ui.settings.SettingsScreen
import com.jaimesorrow.wiccan.ui.theme.ThemeViewModel
import com.jaimesorrow.wiccan.ui.theme.WiccanTheme
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
    private val rootViewModel: RootViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { rootViewModel.rootState.value == RootState.Loading }

        val startOnPaywall = intent?.data == Uri.parse(WiccanDeepLinks.PREMIUM_URI)
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            WiccanTheme(themeMode = themeMode) {
                EdgeToEdgeEffect()
                val rootState by rootViewModel.rootState.collectAsStateWithLifecycle()
                var openLegalDocument by remember { mutableStateOf<LegalDocumentId?>(null) }
                var openAccount by remember(rootState) { mutableStateOf(false) }
                val subscriptionUid = when (val state = rootState) {
                    is RootState.SignedIn -> state.uid
                    is RootState.RequiresSubscription -> state.uid
                    else -> null
                }
                if (subscriptionUid != null) {
                    key(subscriptionUid) { SubscriptionSetupEffect() }
                }

                openLegalDocument?.let { documentId ->
                    BackHandler { openLegalDocument = null }
                    LegalDocumentScreen(
                        document = WiccanLegalDocuments.get(documentId),
                        onBack = { openLegalDocument = null },
                    )
                } ?: when (rootState) {
                    is RootState.SignedIn -> key(subscriptionUid) {
                        NotificationSetupEffect()
                        // Account deletion must finish/sign out even if the
                        // server revokes access and removes this navigation shell.
                        WiccanAppShell(
                            startOnPaywall = startOnPaywall,
                            settingsViewModel = hiltViewModel(key = "subscription-account-$subscriptionUid"),
                        )
                    }
                    is RootState.RequiresSubscription -> key(subscriptionUid) {
                        if (openAccount) {
                            SettingsScreen(
                                accountOnly = true,
                                onBack = { openAccount = false },
                                onTermsClick = { openLegalDocument = LegalDocumentId.TERMS },
                                onPrivacyClick = { openLegalDocument = LegalDocumentId.PRIVACY },
                                onContentSafetyClick = { openLegalDocument = LegalDocumentId.CONTENT_SAFETY },
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
                    RootState.SignedOut -> SignInScreen(onOpenLegalDocument = { openLegalDocument = it })
                    RootState.RequiresPolicyAcceptance ->
                        PolicyAcceptanceScreen(onOpenLegalDocument = { openLegalDocument = it })
                    RootState.Loading, RootState.InitializingProfile -> LoadingIndicator()
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
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
