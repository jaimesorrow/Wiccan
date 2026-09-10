package com.nyxwell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nyxwell.app.ui.notification.NotificationSetupEffect
import com.nyxwell.app.ui.theme.NyxwellTheme
import com.nyxwell.app.ui.today.TodayScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-screen host for the Today experience. Onboarding, settings,
 * archive, and any post-launch navigation are out of scope for this pass.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NyxwellTheme {
                NotificationSetupEffect()
                TodayScreen()
            }
        }
    }
}
