package com.jaimesorrow.wiccan.ads

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.jaimesorrow.wiccan.ui.theme.grimoireGold
import com.wiccan.app.BuildConfig
import com.wiccan.app.R

/**
 * The Grimoire's single ad placement: an adaptive anchored banner at the
 * bottom of the screen, above the app's navigation bar. Renders nothing for
 * subscribers, before consent allows ad requests, or when no ad has loaded
 * (offline or no fill), so it never leaves an empty gap.
 *
 * Self-contained so it works in any bottom slot: pass [showTopRule] = false
 * when the host already draws its own gold hairline (for example a themed
 * `grimoire-banner-slot`), to avoid a double rule.
 */
@Composable
fun GrimoireBannerAd(
    modifier: Modifier = Modifier,
    showTopRule: Boolean = true,
    viewModel: AdsViewModel = hiltViewModel(),
) {
    val tierAllowsAds by viewModel.tierAllowsAds.collectAsStateWithLifecycle()
    val showAds by viewModel.showAds.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    LaunchedEffect(tierAllowsAds, activity) {
        if (tierAllowsAds && activity != null) viewModel.gatherConsent(activity)
    }
    if (showAds) {
        AnchoredBanner(adUnitId = BuildConfig.ADMOB_GRIMOIRE_BANNER_UNIT_ID, showTopRule = showTopRule, modifier = modifier)
    }
}

@Composable
private fun AnchoredBanner(adUnitId: String, showTopRule: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val adSize = remember(context, widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        var loaded by remember(adSize, adUnitId) { mutableStateOf(false) }
        val banner = remember(context, adSize, adUnitId) {
            BannerHolder(context, adSize, adUnitId) { loaded = it }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(banner, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> banner.resume()
                    Lifecycle.Event.ON_PAUSE -> banner.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) banner.resume()
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                banner.destroy()
            }
        }

        GrimoireAdFrame(showChrome = loaded, showTopRule = showTopRule) {
            AndroidView(
                factory = { banner.view },
                modifier = Modifier
                    .fillMaxWidth()
                    // Collapsed until an ad loads; offline or no fill shows nothing.
                    .height(if (loaded) adSize.height.dp else 0.dp)
                    .testTag("grimoire-banner-ad"),
            )
        }
    }
}

/**
 * The themed frame: midnight background, a thin gold rule along the top and
 * a plain "Advertisement" label, so the ad sits flush above the navigation
 * bar and is never mistaken for Wiccan content.
 */
@Composable
internal fun GrimoireAdFrame(showChrome: Boolean, showTopRule: Boolean = true, ad: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showChrome) {
            if (showTopRule) HorizontalDivider(thickness = 0.5.dp, color = grimoireGold().copy(alpha = 0.6f))
            Text(
                text = stringResource(R.string.ad_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp, bottom = 2.dp),
            )
        }
        ad()
    }
}

/**
 * Owns one AdView. Loads on creation, retries on resume after a failure
 * (e.g. the device was offline), and is destroyed with its composition.
 */
private class BannerHolder(
    context: Context,
    adSize: AdSize,
    adUnitId: String,
    private val onLoadedChanged: (Boolean) -> Unit,
) {
    private var destroyed = false
    private var loading = false
    private var loaded = false

    val view: AdView = AdView(context).apply {
        setAdSize(adSize)
        this.adUnitId = adUnitId
        adListener = object : AdListener() {
            override fun onAdLoaded() {
                loading = false
                loaded = true
                onLoadedChanged(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loading = false
                // Keep showing a previously loaded ad; otherwise stay collapsed.
                if (!loaded) onLoadedChanged(false)
            }
        }
    }

    init {
        load()
    }

    private fun load() {
        if (destroyed || loading) return
        loading = true
        try {
            // No location, keywords or user data: the device's coarse location (Home
            // planetary hours) is never passed to AdMob. See docs/ADMOB.md.
            view.loadAd(AdRequest.Builder().build())
        } catch (_: RuntimeException) {
            loading = false
        }
    }

    fun resume() {
        if (destroyed) return
        view.resume()
        if (!loaded) load()
    }

    fun pause() {
        if (!destroyed) view.pause()
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        view.destroy()
    }
}
