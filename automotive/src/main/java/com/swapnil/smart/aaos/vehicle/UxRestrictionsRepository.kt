package com.swapnil.smart.aaos.vehicle

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * The platform's driver-distraction state.
 *
 * The app previously gated its UI purely on its own simulated speed
 * (`isCarMoving`), which is a guess at what the host is doing. This reads the
 * real thing: [CarUxRestrictions.isRequiresDistractionOptimization] and the
 * active restriction flags, pushed by [CarUxRestrictionsManager].
 *
 * Reading and listening need no special permission - only
 * `CAR_UX_RESTRICTIONS_CONFIGURATION`, for *changing* the configuration, is
 * `signature|privileged`.
 *
 * Caveat, measured on this emulator: the values this reports do not match the
 * display the app is on. Display 0 reads `DO: false UxR: 0` while parked, yet
 * this manager reports `requiresDO=true flags=0x1ff` (FULLY_RESTRICTED, which
 * is what display 3, the cluster, reports) and never delivers a change event
 * when display 0's restrictions change. So this is wired up for reporting
 * only - see VehicleViewModel.isInteractionRestricted.
 *
 * That costs nothing: the host enforces the real restrictions itself and will
 * replace the whole app with "You can't use this feature while driving" when
 * it must.
 */
object UxRestrictionsRepository {

    private const val TAG = "SmartAAOS_UXR"

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    private var started = false
    private var acquireAttempts = 0

    private const val MAX_ACQUIRE_ATTEMPTS = 4

    private val listeners = mutableListOf<() -> Unit>()

    /** True when the host requires distraction-optimised content. */
    @Volatile
    var requiresDistractionOptimization: Boolean = false
        private set

    /** Bitmask of [CarUxRestrictions] UX_RESTRICTIONS_* flags. */
    @Volatile
    var activeRestrictions: Int = 0
        private set

    /**
     * Host cap on list items while restricted, or -1 if unknown. Useful when
     * deciding how much to put in a ListTemplate.
     */
    @Volatile
    var maxCumulativeContentItems: Int = -1
        private set

    fun observe(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeObserver(listener: () -> Unit) {
        listeners.remove(listener)
    }

    /** Idempotent. */
    fun start(context: Context) {
        if (started) return
        started = true
        Log.d(TAG, "start() - creating Car for UX restrictions")
        try {
            car = Car.createCar(
                context,
                Handler(Looper.getMainLooper()),
                Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER
            ) { _, connected ->
                Log.d(TAG, "Car lifecycle: connected=$connected")
                if (connected) acquire() else manager = null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Car unavailable: ${e.message}")
        }
    }

    private fun acquire() {
        if (manager != null) return
        // The manager is not available at the moment the connection callback
        // fires - it needs a retry, unlike CarPropertyManager.
        val uxr = try {
            car?.getCarManager(CarUxRestrictionsManager::class.java)
        } catch (e: Exception) {
            Log.d(TAG, "getCarManager(CarUxRestrictionsManager) failed: ${e.message}")
            null
        }
        if (uxr == null) {
            // Some managers become available slightly after the connection
            // callback. Retry a few times before giving up.
            if (acquireAttempts < MAX_ACQUIRE_ATTEMPTS) {
                acquireAttempts++
                Log.d(TAG, "Manager not ready, retry $acquireAttempts/$MAX_ACQUIRE_ATTEMPTS")
                Handler(Looper.getMainLooper()).postDelayed({ acquire() }, 1500)
            } else {
                Log.d(TAG, "CarUxRestrictionsManager unavailable after $acquireAttempts retries")
            }
            return
        }
        manager = uxr

        try {
            uxr.registerListener { restrictions -> apply(restrictions) }
            // registerListener does not deliver the current value, so seed it.
            uxr.currentCarUxRestrictions?.let { apply(it) }
            Log.d(TAG, "Listening for UX restriction changes")
        } catch (e: Exception) {
            Log.d(TAG, "registerListener failed: ${e.message}")
        }
    }

    private fun apply(restrictions: CarUxRestrictions) {
        requiresDistractionOptimization = restrictions.isRequiresDistractionOptimization
        activeRestrictions = restrictions.activeRestrictions
        maxCumulativeContentItems = try {
            restrictions.maxCumulativeContentItems
        } catch (e: Exception) {
            -1
        }
        Log.d(
            TAG,
            "UXR: requiresDO=$requiresDistractionOptimization " +
                "flags=0x${Integer.toHexString(activeRestrictions)} " +
                "maxItems=$maxCumulativeContentItems"
        )
        listeners.toList().forEach { it() }
    }

    fun release() {
        try {
            manager?.unregisterListener()
            car?.disconnect()
        } catch (e: Exception) {
            Log.d(TAG, "release failed: ${e.message}")
        }
        manager = null
        car = null
        started = false
    }
}
