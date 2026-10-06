package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation

/** Reconcile purchases after sign-in and each return to the foreground. */
@Composable
fun SubscriptionSetupEffect(viewModel: SubscriptionSetupViewModel = hiltViewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshPurchases()
            awaitCancellation()
        }
    }
}
