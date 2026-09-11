package com.swapnil.smart.aaos.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log

/**
 * Current vehicle position, used to anchor the map and to compute real
 * distances instead of the hardcoded ones baked into [
 * com.swapnil.smart.aaos.ui.screens.NavDestination].
 *
 * On the emulator, feed it a position with:
 *   adb emu geo fix <longitude> <latitude>      # note: lon first
 *   adb shell cmd location set-location-enabled true --user 10
 */
object CarLocationProvider {

    private const val TAG = "SmartAAOS_Loc"

    const val PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION

    /** Last known position, or null until a fix arrives. */
    @Volatile
    var current: Location? = null
        private set

    private var manager: LocationManager? = null
    private var listener: LocationListener? = null

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Providers to try, best first. FUSED only exists from API 31. */
    private fun providers(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Seeds from the system's last known fix, then subscribes for updates.
     * [onUpdate] fires on the main thread whenever the position improves.
     * Safe to call repeatedly.
     */
    fun start(context: Context, onUpdate: () -> Unit) {
        if (!hasPermission(context)) {
            Log.d(TAG, "Location permission not granted - map stays on fallback origin")
            return
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        manager = lm

        providers().forEach { provider ->
            try {
                if (!lm.isProviderEnabled(provider)) return@forEach
                lm.getLastKnownLocation(provider)?.let { accept(it) }
            } catch (e: Exception) {
                Log.d(TAG, "getLastKnownLocation($provider) failed: ${e.message}")
            }
        }
        current?.let { Log.d(TAG, "Seeded fix: ${it.latitude}, ${it.longitude} (${it.provider})") }
        onUpdate()

        if (listener != null) return
        val l = LocationListener { location ->
            if (accept(location)) {
                Log.d(TAG, "Fix update: ${location.latitude}, ${location.longitude}")
                onUpdate()
            }
        }
        listener = l
        providers().forEach { provider ->
            try {
                if (!lm.isProviderEnabled(provider)) return@forEach
                lm.requestLocationUpdates(provider, 2000L, 5f, l, Looper.getMainLooper())
            } catch (e: Exception) {
                Log.d(TAG, "requestLocationUpdates($provider) failed: ${e.message}")
            }
        }
    }

    /** Keeps the newest fix. Returns true when [current] changed. */
    private fun accept(location: Location): Boolean {
        val existing = current
        if (existing != null && location.time < existing.time) return false
        current = location
        return true
    }

    fun stop() {
        val l = listener ?: return
        try {
            manager?.removeUpdates(l)
        } catch (e: Exception) {
            Log.d(TAG, "removeUpdates failed: ${e.message}")
        }
        listener = null
    }

    /** Straight-line distance in km from the current fix, or null without one. */
    fun distanceKmTo(lat: Double, lng: Double): Double? {
        val origin = current ?: return null
        val out = FloatArray(1)
        Location.distanceBetween(origin.latitude, origin.longitude, lat, lng, out)
        return out[0] / 1000.0
    }
}
