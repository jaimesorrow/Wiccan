package com.nyxwell.app.ui.subscription

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.nyxwell.app.core.util.findActivity

/**
 * Bare functional CTA for the single premium tier — deliberately
 * undesigned (no paywall screen in this pass). Where this lives in the
 * app's navigation is a decision for whichever screen adopts it; this
 * pass only makes the trigger available for that screen to place.
 */
@Composable
fun UpgradeButton(modifier: Modifier = Modifier, viewModel: SubscriptionViewModel = hiltViewModel()) {
    val context = LocalContext.current
    Button(
        modifier = modifier,
        onClick = { context.findActivity()?.let(viewModel::upgrade) },
    ) {
        Text("Upgrade")
    }
}
