package com.jaimesorrow.wiccan.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaimesorrow.wiccan.core.config.PolicyUrls
import com.jaimesorrow.wiccan.core.error.toUserMessage
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanEmblem
import com.jaimesorrow.wiccan.ui.brand.wiccanEmblemColor
import com.jaimesorrow.wiccan.ui.brand.wiccanPageWash
import com.jaimesorrow.wiccan.ui.theme.GrimoireChevronRow
import com.jaimesorrow.wiccan.ui.theme.GrimoireSectionHeader
import com.jaimesorrow.wiccan.ui.theme.MidnightGrimoireTheme
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.jaimesorrow.wiccan.ui.theme.description
import com.jaimesorrow.wiccan.ui.theme.displayName
import com.jaimesorrow.wiccan.ui.theme.grimoireFieldColors
import com.jaimesorrow.wiccan.ui.theme.grimoireSwitchColors
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
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val loadingDescription = stringResource(R.string.loading_description)
    val subscriptionUnavailable = stringResource(R.string.settings_manage_subscription_unavailable)
    val contactUnavailable = stringResource(R.string.settings_contact_unavailable, PolicyUrls.SUPPORT_EMAIL)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingAccount by remember { mutableStateOf(false) }

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

    MidnightGrimoireTheme {
        if (state.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsUiEvent.CancelDeleteAccount) },
                icon = { WiccanEmblem(size = 32.dp) },
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
                    .wiccanPageWash()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                if (editingAccount) {
                    AccountNameEditor(
                        state = state,
                        loadingDescription = loadingDescription,
                        onBack = { editingAccount = false },
                        onEvent = viewModel::onEvent,
                    )
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                    if (accountOnly) TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back)) }
                    WiccanEmblem(
                        modifier = Modifier.align(Alignment.CenterHorizontally).testTag("settings-logo"),
                        size = 44.dp,
                        contentDescription = stringResource(R.string.settings_logo_description),
                    )
                    WiccanBotanicalAccent(modifier = Modifier.align(Alignment.CenterHorizontally), width = 116.dp)
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
                    if (!accountOnly) {
                        GrimoireSectionHeader(stringResource(R.string.settings_section_spiritual))
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
                                colors = grimoireSwitchColors(),
                            )
                        }

                        GrimoireSectionHeader(stringResource(R.string.settings_section_grimoire))
                        Text(
                            text = stringResource(R.string.settings_palette),
                            style = MaterialTheme.typography.titleLarge,
                            color = wiccanEmblemColor(),
                            modifier = Modifier.semantics { heading() },
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
                    }

                    GrimoireSectionHeader(stringResource(R.string.settings_section_data))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingAccount = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_account_heading),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = stringResource(R.string.settings_email, state.accountEmail),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "›",
                            color = WiccanPaletteColors.Grimoire.Gold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
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
                        Switch(checked = state.shareAnalytics, onCheckedChange = null, colors = grimoireSwitchColors())
                    }

                    Spacer(modifier = Modifier.height(12.dp))
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
                    Spacer(modifier = Modifier.height(12.dp))
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

                    Spacer(modifier = Modifier.height(8.dp))
                    GrimoireSectionHeader(stringResource(R.string.settings_section_circle))
                    GrimoireChevronRow(stringResource(R.string.policy_terms), onClick = onTermsClick)
                    GrimoireChevronRow(stringResource(R.string.policy_privacy), onClick = onPrivacyClick)
                    GrimoireChevronRow(stringResource(R.string.policy_safety), onClick = onContentSafetyClick)
                    GrimoireChevronRow(stringResource(R.string.policy_potion_safety), onClick = onPotionSafetyClick)
                    GrimoireChevronRow(stringResource(R.string.settings_contact)) {
                        openExternal(supportEmailIntent(), contactUnavailable)
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
    }
}

@Composable
private fun AccountNameEditor(
    state: SettingsUiState,
    loadingDescription: String,
    onBack: () -> Unit,
    onEvent: (SettingsUiEvent) -> Unit,
) {
    Spacer(modifier = Modifier.height(16.dp))
    TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back)) }
    Text(
        stringResource(R.string.settings_account_heading),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.settings_email, state.accountEmail),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = state.firstName,
        onValueChange = { onEvent(SettingsUiEvent.FirstNameChanged(it)) },
        label = { Text(stringResource(R.string.onboarding_first_name)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = grimoireFieldColors(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = state.lastName,
        onValueChange = { onEvent(SettingsUiEvent.LastNameChanged(it)) },
        label = { Text(stringResource(R.string.onboarding_last_name)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = grimoireFieldColors(),
    )
    OutlinedButton(
        onClick = { onEvent(SettingsUiEvent.SaveNameClicked) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isAccountBusy,
    ) { Text(stringResource(R.string.settings_save_name)) }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = state.newEmail,
        onValueChange = { onEvent(SettingsUiEvent.NewEmailChanged(it)) },
        label = { Text(stringResource(R.string.settings_new_email)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = grimoireFieldColors(),
    )
    OutlinedButton(
        onClick = { onEvent(SettingsUiEvent.ChangeEmailClicked) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isAccountBusy,
    ) { Text(stringResource(R.string.settings_verify_email)) }
    OutlinedButton(
        onClick = { onEvent(SettingsUiEvent.ResetPasswordClicked) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isAccountBusy,
    ) { Text(stringResource(R.string.settings_reset_password)) }
    if (state.isAccountBusy) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = loadingDescription },
        )
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
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
        ?: context.getString(R.string.settings_version_unknown)
} catch (t: Throwable) {
    context.getString(R.string.settings_version_unknown)
}
