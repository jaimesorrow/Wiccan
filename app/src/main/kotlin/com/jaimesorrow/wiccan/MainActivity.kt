package com.jaimesorrow.wiccan

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.domain.model.RootState
import com.jaimesorrow.wiccan.ui.auth.RootViewModel
import com.jaimesorrow.wiccan.ui.auth.SignInScreen
import com.jaimesorrow.wiccan.ui.navigation.WiccanAppShell
import com.jaimesorrow.wiccan.ui.navigation.WiccanDeepLinks
import com.jaimesorrow.wiccan.ui.notification.NotificationSetupEffect
import com.jaimesorrow.wiccan.ui.policy.PolicyAcceptanceScreen
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

                when (rootState) {
                    RootState.SignedIn -> {
                        NotificationSetupEffect()
                        WiccanAppShell(startOnPaywall = startOnPaywall)
                    }
                    RootState.SignedOut -> SignInScreen()
                    RootState.RequiresPolicyAcceptance -> PolicyAcceptanceScreen()
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
