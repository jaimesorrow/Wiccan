package com.wiccan.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiccan.app.domain.model.RootState
import com.wiccan.app.ui.auth.RootViewModel
import com.wiccan.app.ui.auth.SignInScreen
import com.wiccan.app.ui.navigation.WiccanAppShell
import com.wiccan.app.ui.navigation.WiccanDeepLinks
import com.wiccan.app.ui.notification.NotificationSetupEffect
import com.wiccan.app.ui.theme.WiccanTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Root host, resolved via [RootViewModel.rootState]: signed-out shows the
 * sign-in screen; signed-in (with a ready profile) hosts the full
 * navigation shell ([WiccanAppShell]). No fixed timer or `currentUser
 * != null` shortcut — the AndroidX SplashScreen stays on screen (per
 * [Theme.Wiccan.Starting]) until [RootState.Loading] resolves to
 * something real.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { rootViewModel.rootState.value == RootState.Loading }

        enableEdgeToEdge()
        val startOnPaywall = intent?.data == Uri.parse(WiccanDeepLinks.PREMIUM_URI)
        setContent {
            WiccanTheme {
                val rootState by rootViewModel.rootState.collectAsStateWithLifecycle()

                when (rootState) {
                    RootState.SignedIn -> {
                        NotificationSetupEffect()
                        WiccanAppShell(startOnPaywall = startOnPaywall)
                    }
                    RootState.SignedOut -> SignInScreen()
                    RootState.Loading,
                    RootState.InitializingProfile,
                    RootState.RequiresPolicyAcceptance,
                    -> LoadingIndicator()
                }
            }
        }
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
