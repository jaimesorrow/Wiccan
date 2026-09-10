package com.nyxwell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyxwell.app.domain.model.AuthState
import com.nyxwell.app.ui.auth.RootViewModel
import com.nyxwell.app.ui.auth.SignInScreen
import com.nyxwell.app.ui.notification.NotificationSetupEffect
import com.nyxwell.app.ui.theme.NyxwellTheme
import com.nyxwell.app.ui.today.TodayScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Root host, auth-aware: signed-out shows the sign-in screen, signed-in
 * shows Today. No navigation graph yet (no other screens exist) — that
 * arrives once there's more than one signed-in destination.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NyxwellTheme {
                val rootViewModel: RootViewModel = hiltViewModel()
                val authState by rootViewModel.authState.collectAsStateWithLifecycle()

                when (authState) {
                    is AuthState.SignedIn -> {
                        NotificationSetupEffect()
                        TodayScreen()
                    }
                    AuthState.SignedOut -> SignInScreen()
                }
            }
        }
    }
}
