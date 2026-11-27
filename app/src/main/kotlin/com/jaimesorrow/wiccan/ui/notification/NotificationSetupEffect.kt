package com.jaimesorrow.wiccan.ui.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiccan.app.BuildConfig
import com.wiccan.app.R

/**
 * Requests the Android 13+ POST_NOTIFICATIONS runtime permission (a no-op
 * on older versions, where notification permission is granted at
 * install), then registers the device's FCM token once permission is
 * available. Call once from the signed-in root so this explanation is only
 * shown after purchase verification has unlocked the app.
 */
@Composable
fun NotificationSetupEffect(viewModel: NotificationSetupViewModel = hiltViewModel()) {
    // FCM has no local Emulator Suite service. Do not request permission or
    // register a real production token while the rest of the app is local.
    if (BuildConfig.USE_FIREBASE_EMULATORS) return

    val context = LocalContext.current
    var showPermissionExplanation by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onPermissionGranted()
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            viewModel.onPermissionGranted()
        } else {
            showPermissionExplanation = true
        }
    }

    if (showPermissionExplanation) {
        NotificationPermissionExplanation(
            onContinue = {
                showPermissionExplanation = false
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onNotNow = { showPermissionExplanation = false },
        )
    }
}

@Composable
internal fun NotificationPermissionExplanation(
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onNotNow,
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = { Text(stringResource(R.string.notification_permission_body)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.notification_permission_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onNotNow) {
                Text(stringResource(R.string.notification_permission_not_now))
            }
        },
    )
}
