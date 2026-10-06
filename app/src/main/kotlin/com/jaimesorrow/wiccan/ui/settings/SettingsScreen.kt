package com.jaimesorrow.wiccan.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.config.PolicyUrls
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import com.jaimesorrow.wiccan.ui.theme.description
import com.jaimesorrow.wiccan.ui.theme.displayName

/**
 * Account settings: sign out, manage the subscription (deep link to Play
 * for premium, or an entry to the Paywall for free), toggle the daily
 * notification, and delete the account.
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
                .verticalScroll(rememberScrollState())
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

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Daily Notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsUiEvent.NotificationsToggled(it)) },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "The seasonal palette is the recommended default, but you can lock any of the six.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ThemeModePicker(
                selected = state.themeMode,
                onSelected = { viewModel.onEvent(SettingsUiEvent.ThemeModeSelected(it)) },
            )

            Spacer(modifier = Modifier.height(24.dp))
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
                TextButton(onClick = { context.startActivity(supportEmailIntent()) }) {
                    Text("Contact Support", style = MaterialTheme.typography.labelSmall)
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

/** Kept internal (not private) so the theme package's `@Preview` functions can render it standalone. */
@Composable
internal fun ThemeModePicker(
    selected: WiccanThemeMode,
    onSelected: (WiccanThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        WiccanThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == selected,
                        onClick = { onSelected(mode) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == selected, onClick = null)
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = mode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun manageSubscriptionIntent(): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))

private fun supportEmailIntent(): Intent =
    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${PolicyUrls.SUPPORT_EMAIL}"))

private fun appVersionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
} catch (t: Throwable) {
    "unknown"
}
