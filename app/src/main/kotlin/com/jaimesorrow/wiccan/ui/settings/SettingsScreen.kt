package com.jaimesorrow.wiccan.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.config.PolicyUrls
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import com.jaimesorrow.wiccan.notification.manageSubscriptionIntent
import com.jaimesorrow.wiccan.ui.brand.WiccanEmblem
import com.jaimesorrow.wiccan.ui.theme.description
import com.jaimesorrow.wiccan.ui.theme.displayName
import com.wiccan.app.R
import kotlinx.coroutines.launch

/**
 * Account settings and the restricted account/support surface available
 * before subscription. Neither surface offers a free content plan.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    accountOnly: Boolean = false,
    onBack: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onContentSafetyClick: () -> Unit = {},
    onPotionSafetyClick: () -> Unit = {},
    // Ad "Privacy choices" entry (UMP); production wires AdPrivacyChoicesRow.
    privacyChoices: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val loadingDescription = stringResource(R.string.loading_description)
    val subscriptionUnavailable = stringResource(R.string.settings_manage_subscription_unavailable)
    val contactUnavailable = stringResource(R.string.settings_contact_unavailable, PolicyUrls.CUSTOMER_SERVICE_EMAIL)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Devices without an email client or browser must not crash on these links.
    fun openExternal(intent: Intent, unavailableMessage: String) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar(unavailableMessage) }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.error.toUserMessage(resources))
                is SettingsUiEffect.ShowMessage -> snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
            }
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(SettingsUiEvent.CancelDeleteAccount) },
            title = { Text(stringResource(R.string.settings_delete_title)) },
            text = { Text(stringResource(R.string.settings_delete_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(SettingsUiEvent.ConfirmDeleteAccount) }) {
                    Text(stringResource(R.string.settings_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(SettingsUiEvent.CancelDeleteAccount) }) {
                    Text(stringResource(R.string.settings_delete_cancel))
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
            if (accountOnly) TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back)) }
            WiccanEmblem(
                modifier = Modifier.align(Alignment.CenterHorizontally).testTag("settings-logo"),
                size = 44.dp,
                contentDescription = stringResource(R.string.settings_logo_description),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(if (accountOnly) R.string.settings_account_title else R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(if (accountOnly) R.string.settings_account_subtitle else R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.settings_account_heading), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_email, state.accountEmail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.firstName,
                onValueChange = { viewModel.onEvent(SettingsUiEvent.FirstNameChanged(it)) },
                label = { Text(stringResource(R.string.onboarding_first_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.lastName,
                onValueChange = { viewModel.onEvent(SettingsUiEvent.LastNameChanged(it)) },
                label = { Text(stringResource(R.string.onboarding_last_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsUiEvent.SaveNameClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isAccountBusy,
            ) { Text(stringResource(R.string.settings_save_name)) }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.newEmail,
                onValueChange = { viewModel.onEvent(SettingsUiEvent.NewEmailChanged(it)) },
                label = { Text(stringResource(R.string.settings_new_email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsUiEvent.ChangeEmailClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isAccountBusy,
            ) { Text(stringResource(R.string.settings_verify_email)) }
            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsUiEvent.ResetPasswordClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isAccountBusy,
            ) { Text(stringResource(R.string.settings_reset_password)) }
            if (state.isAccountBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsUiEvent.SignOutClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_sign_out))
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    openExternal(
                        manageSubscriptionIntent(),
                        subscriptionUnavailable,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_manage_subscription))
            }

            state.yearlySwitchOffer?.let { offer ->
                Spacer(modifier = Modifier.height(12.dp))
                SwitchToYearlyRow(
                    offer = offer,
                    enabled = !state.isSwitchBusy && !state.isAccountBusy && !state.isDeleting,
                    onClick = { context.findActivity()?.let(viewModel::switchToYearly) },
                )
            }

            if (!accountOnly) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = state.notificationsEnabled,
                            enabled = !state.isNotificationBusy,
                            role = Role.Switch,
                            onValueChange = { viewModel.onEvent(SettingsUiEvent.NotificationsToggled(it)) },
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.settings_notifications),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = stringResource(R.string.settings_notifications_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = null,
                        enabled = !state.isNotificationBusy,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.settings_palette),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_palette_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThemeModePicker(
                    selected = state.themeMode,
                    onSelected = { viewModel.onEvent(SettingsUiEvent.ThemeModeSelected(it)) },
                )

                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.settings_language_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppLanguagePicker(
                    selected = AppLanguageManager.selected(context),
                    onSelected = { language ->
                        context.findActivity()?.let { AppLanguageManager.select(it, language) }
                        viewModel.onEvent(SettingsUiEvent.LanguageChanged)
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.shareAnalytics,
                        role = Role.Switch,
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.ShareAnalyticsToggled(it)) },
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_analytics),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.settings_analytics_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.shareAnalytics, onCheckedChange = null)
            }
            privacyChoices()

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsUiEvent.DeleteAccountClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isDeleting,
            ) {
                Text(stringResource(R.string.settings_delete))
            }

            if (state.isDeleting) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onTermsClick) {
                    Text(stringResource(R.string.policy_terms), style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onPrivacyClick) {
                    Text(stringResource(R.string.policy_privacy), style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onContentSafetyClick) {
                    Text(stringResource(R.string.policy_safety), style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onPotionSafetyClick) {
                    Text(stringResource(R.string.policy_potion_safety), style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(
                    onClick = {
                        openExternal(
                            supportEmailIntent(),
                            contactUnavailable,
                        )
                    },
                ) {
                    Text(stringResource(R.string.settings_contact), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                text = stringResource(R.string.settings_version, appVersionName(context)),
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

@Composable
internal fun AppLanguagePicker(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup().testTag("language-picker")) {
        AppLanguage.entries.forEach { language ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = language == selected,
                        onClick = { onSelected(language) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = language == selected, onClick = null)
                Text(
                    text = stringResource(language.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private fun supportEmailIntent(): Intent =
    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${PolicyUrls.CUSTOMER_SERVICE_EMAIL}"))

private fun appVersionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
        ?: context.getString(R.string.settings_version_unknown)
} catch (t: Throwable) {
    context.getString(R.string.settings_version_unknown)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
