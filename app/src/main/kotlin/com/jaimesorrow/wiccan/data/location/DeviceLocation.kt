package com.jaimesorrow.wiccan.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.jaimesorrow.wiccan.sky.GeoCoordinate
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

fun hasCoarseLocationPermission(context: Context): Boolean {
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    return coarse == PackageManager.PERMISSION_GRANTED || fine == PackageManager.PERMISSION_GRANTED
}

/** Last coarse fix, or null when permission or a provider fix is unavailable. */
fun lastKnownCoordinate(context: Context): GeoCoordinate? {
    if (!hasCoarseLocationPermission(context)) return null
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    val location = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    ).mapNotNull { provider ->
        runCatching { if (manager.isProviderEnabled(provider)) manager.getLastKnownLocation(provider) else null }
            .getOrNull()
    }.maxByOrNull { it.time } ?: return null
    return GeoCoordinate(location.latitude, location.longitude)
}

/** Rounds to hundredths of a degree so repeated lookups do not churn on tiny GPS jitter. */
fun GeoCoordinate.roundedForStationLookup(): GeoCoordinate = GeoCoordinate(
    (latitudeDegrees * 100).toInt() / 100.0,
    (longitudeDegrees * 100).toInt() / 100.0,
)

/**
 * Returns a last-known fix when available; otherwise requests one coarse network update
 * (still on-device only).
 */
suspend fun resolveCoarseCoordinate(context: Context): GeoCoordinate? {
    lastKnownCoordinate(context)?.let { return it }
    if (!hasCoarseLocationPermission(context)) return null
    return requestFreshCoarseCoordinate(context)
}

@SuppressLint("MissingPermission")
private suspend fun requestFreshCoarseCoordinate(context: Context): GeoCoordinate? =
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val manager = context.getSystemService(LocationManager::class.java)
            if (manager == null || !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            val handler = Handler(Looper.getMainLooper())
            var delivered = false
            lateinit var listener: LocationListener
            val timeout = Runnable {
                if (!delivered && cont.isActive) {
                    delivered = true
                    manager.removeUpdates(listener)
                    cont.resume(null)
                }
            }
            listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!delivered && cont.isActive) {
                        delivered = true
                        handler.removeCallbacks(timeout)
                        manager.removeUpdates(this)
                        cont.resume(GeoCoordinate(location.latitude, location.longitude))
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit
            }
            manager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0f,
                listener,
                Looper.getMainLooper(),
            )
            handler.postDelayed(timeout, FRESH_LOCATION_TIMEOUT_MS)
            cont.invokeOnCancellation {
                handler.removeCallbacks(timeout)
                manager.removeUpdates(listener)
            }
        }
    }

private const val FRESH_LOCATION_TIMEOUT_MS = 8_000L
