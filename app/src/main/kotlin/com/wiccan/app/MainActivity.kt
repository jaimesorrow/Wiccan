package com.wiccan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiccan.app.domain.model.AuthState
import com.wiccan.app.ui.auth.RootViewModel
import com.wiccan.app.ui.auth.SignInScreen
import com.wiccan.app.ui.notification.NotificationSetupEffect
import com.wiccan.app.ui.paywall.PaywallScreen
import com.wiccan.app.ui.theme.WiccanTheme
import com.wiccan.app.ui.today.TodayScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Root host, auth-aware: signed-out shows the sign-in screen, signed-in
 * shows Today (or the Paywall, when launched with [EXTRA_OPEN_PAYWALL] —
 * the locked widget's entry point). No navigation graph yet beyond that —
 * arrives once there are more signed-in destinations to route between.
 *
 * The AndroidX SplashScreen stays on screen (per [Theme.Wiccan.Starting])
 * until [RootViewModel.authState] resolves past its initial `null` — a
 * fixed timer would either flash blank content or wait longer than needed.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { rootViewModel.authState.value == null }

        enableEdgeToEdge()
        val openPaywall = intent.getBooleanExtra(EXTRA_OPEN_PAYWALL, false)
        setContent {
            WiccanTheme {
                val authState by rootViewModel.authState.collectAsStateWithLifecycle()
                var showPaywall by rememberSaveable { mutableStateOf(openPaywall) }

                when (authState) {
                    is AuthState.SignedIn -> {
                        NotificationSetupEffect()
                        if (showPaywall) {
                            PaywallScreen()
                        } else {
                            TodayScreen()
                        }
                    }
                    AuthState.SignedOut -> SignInScreen()
                    null -> Unit // still resolving; the splash screen is covering this frame
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_PAYWALL = "com.wiccan.app.EXTRA_OPEN_PAYWALL"
    }
}
