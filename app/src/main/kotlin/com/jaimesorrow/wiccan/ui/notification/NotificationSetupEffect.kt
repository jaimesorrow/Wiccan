package com.jaimesorrow.wiccan.ui.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiccan.app.BuildConfig

/**
 * Requests the Android 13+ POST_NOTIFICATIONS runtime permission (a no-op
 * on older versions, where notification permission is granted at
 * install), then registers the device's FCM token once permission is
 * available. Call once near the app's root — there is no dedicated
 * settings screen for this yet.
 */
@Composable
fun NotificationSetupEffect(viewModel: NotificationSetupViewModel = hiltViewModel()) {
    // FCM has no local Emulator Suite service. Do not request permission or
    // register a real production token while the rest of the app is local.
    if (BuildConfig.USE_FIREBASE_EMULATORS) return

    val context = LocalContext.current

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
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
