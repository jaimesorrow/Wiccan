package com.wiccan.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiccan.app.billing.PREMIUM_MONTHLY_PRODUCT_ID
import com.wiccan.app.core.config.PolicyUrls
import com.wiccan.app.core.error.toUserMessage
import com.wiccan.app.domain.model.ContentTier

private const val ANDROID_PACKAGE_NAME = "com.wiccan.app"

/**
 * Account settings: sign out, manage the subscription (deep link to Play
 * for premium, or an entry to the Paywall for free), and delete the
 * account. No notification-time preferences or other settings live here —
 * those belong to later sessions.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onGoPremiumClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.error.toUserMessage())
            }
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(SettingsUiEvent.CancelDeleteAccount) },
            title = { Text("Delete account?") },
            text = { Text("This permanently deletes your account and delivery history. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(SettingsUiEvent.ConfirmDeleteAccount) }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(SettingsUiEvent.CancelDeleteAccount) }) {
                    Text("Cancel")
                }
            },
        )
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
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsUiEvent.SignOutClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign Out")
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    if (state.tier == ContentTier.PREMIUM) {
                        context.startActivity(manageSubscriptionIntent())
                    } else {
                        onGoPremiumClick()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.tier == ContentTier.PREMIUM) "Manage Subscription" else "Go Premium")
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsUiEvent.DeleteAccountClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isDeleting,
            ) {
                Text("Delete Account")
            }

            if (state.isDeleting) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PolicyUrls.TERMS_OF_SERVICE))) }) {
                    Text("Terms of Service", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PolicyUrls.PRIVACY_POLICY))) }) {
                    Text("Privacy Policy", style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                text = "Wiccan v${appVersionName(context)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )
        }
    }
}

private fun manageSubscriptionIntent(): Intent {
    val url = "https://play.google.com/store/account/subscriptions" +
        "?sku=$PREMIUM_MONTHLY_PRODUCT_ID&package=$ANDROID_PACKAGE_NAME"
    return Intent(Intent.ACTION_VIEW, Uri.parse(url))
}

private fun appVersionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
} catch (t: Throwable) {
    "unknown"
}
