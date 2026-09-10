package com.nyxwell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyxwell.app.domain.model.AuthState
import com.nyxwell.app.ui.auth.RootViewModel
import com.nyxwell.app.ui.auth.SignInScreen
import com.nyxwell.app.ui.notification.NotificationSetupEffect
import com.nyxwell.app.ui.paywall.PaywallScreen
import com.nyxwell.app.ui.theme.NyxwellTheme
import com.nyxwell.app.ui.today.TodayScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Root host, auth-aware: signed-out shows the sign-in screen, signed-in
 * shows Today (or the Paywall, when launched with [EXTRA_OPEN_PAYWALL] —
 * the locked widget's entry point). No navigation graph yet beyond that —
 * arrives once there are more signed-in destinations to route between.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openPaywall = intent.getBooleanExtra(EXTRA_OPEN_PAYWALL, false)
        setContent {
            NyxwellTheme {
                val rootViewModel: RootViewModel = hiltViewModel()
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
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_PAYWALL = "com.nyxwell.app.EXTRA_OPEN_PAYWALL"
    }
}
