package com.jaimesorrow.wiccan.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.jaimesorrow.wiccan.sky.GeoCoordinate

/** Last coarse fix, or null when permission or a provider fix is unavailable. */
fun lastKnownCoordinate(context: Context): GeoCoordinate? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return null
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
