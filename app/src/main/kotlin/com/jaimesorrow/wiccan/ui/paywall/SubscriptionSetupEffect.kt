package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.jaimesorrow.wiccan.core.util.findActivity
import kotlinx.coroutines.awaitCancellation

/** Reconcile purchases after sign-in and each return to the foreground. */
@Composable
fun SubscriptionSetupEffect(viewModel: SubscriptionSetupViewModel = hiltViewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current.findActivity()
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            activity?.let { viewModel.showTransactionalMessage(it) }
            viewModel.refreshPurchases()
            awaitCancellation()
        }
    }
}
