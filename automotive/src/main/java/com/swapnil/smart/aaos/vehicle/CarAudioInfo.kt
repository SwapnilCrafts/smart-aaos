package com.swapnil.smart.aaos.vehicle

import android.car.Car
import android.car.media.CarAudioManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * The parts of AAOS car audio a third-party app can actually reach.
 *
 * Multi-zone audio is often listed as an automotive skill, but almost all of
 * [CarAudioManager] is `@SystemApi`: audio zones, volume groups,
 * `setGroupVolume`, `getVolumeGroupCount` are not in the public SDK, and both
 * `CAR_CONTROL_AUDIO_VOLUME` and `CAR_CONTROL_AUDIO_SETTINGS` are
 * `signature|privileged`. What a normal app gets is exactly three methods:
 * [CarAudioManager.isAudioFeatureEnabled] and register/unregister of a
 * [CarAudioManager.CarVolumeCallback].
 *
 * So the app-side half of car audio is really about declaring the right
 * `AudioAttributes.USAGE_*` and handling focus correctly - the car's audio
 * policy is what maps a usage onto a bus, zone and volume group. This class
 * reports what the platform supports and observes volume changes; the routing
 * decisions live in SmartMusicService's audio attributes.
 */
class CarAudioInfo(context: Context) {

    companion object {
        private const val TAG = "SmartAAOS_Audio"

        /** Feature flags worth reporting, newest last. */
        private val FEATURES = listOf(
            "DYNAMIC_ROUTING" to CarAudioManager.AUDIO_FEATURE_DYNAMIC_ROUTING,
            "VOLUME_GROUP_MUTING" to CarAudioManager.AUDIO_FEATURE_VOLUME_GROUP_MUTING,
            "VOLUME_GROUP_EVENTS" to CarAudioManager.AUDIO_FEATURE_VOLUME_GROUP_EVENTS,
            "AUDIO_MIRRORING" to CarAudioManager.AUDIO_FEATURE_AUDIO_MIRRORING,
            "OEM_AUDIO_SERVICE" to CarAudioManager.AUDIO_FEATURE_OEM_AUDIO_SERVICE
        )
    }

    private var car: Car? = null
    private var carAudioManager: CarAudioManager? = null
    private var volumeCallback: CarAudioManager.CarVolumeCallback? = null

    init {
        try {
            car = Car.createCar(
                context,
                Handler(Looper.getMainLooper()),
                Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER
            ) { _, connected ->
                if (connected) acquire() else carAudioManager = null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Car audio unavailable: ${e.message}")
        }
    }

    private fun acquire() {
        if (carAudioManager != null) return
        carAudioManager = try {
            car?.getCarManager(Car.AUDIO_SERVICE) as? CarAudioManager
        } catch (e: Exception) {
            Log.d(TAG, "getCarManager(AUDIO_SERVICE) failed: ${e.message}")
            null
        }
    }

    /** Logs which car audio features this platform reports. */
    fun logCapabilities() {
        acquire()
        val manager = carAudioManager ?: run {
            Log.d(TAG, "CarAudioManager unavailable - no car audio info")
            return
        }
        FEATURES.forEach { (name, flag) ->
            val enabled = try {
                manager.isAudioFeatureEnabled(flag)
            } catch (e: Exception) {
                Log.d(TAG, "isAudioFeatureEnabled($name) threw: ${e.message}")
                null
            }
            Log.d(TAG, "audio feature %-20s %s".format(name, enabled ?: "unknown"))
        }
    }

    /**
     * Observes car volume and mute changes. This is the one car-audio callback
     * open to a normal app; if the platform rejects it, that is logged and the
     * app carries on (playback does not depend on it).
     */
    fun startObservingVolume() {
        acquire()
        val manager = carAudioManager ?: return
        if (volumeCallback != null) return

        val callback = object : CarAudioManager.CarVolumeCallback() {
            override fun onGroupVolumeChanged(zoneId: Int, groupId: Int, flags: Int) {
                Log.d(TAG, "volume changed: zone=$zoneId group=$groupId")
            }

            override fun onGroupMuteChanged(zoneId: Int, groupId: Int, flags: Int) {
                Log.d(TAG, "mute changed: zone=$zoneId group=$groupId")
            }

            override fun onMasterMuteChanged(zoneId: Int, flags: Int) {
                Log.d(TAG, "master mute changed: zone=$zoneId")
            }
        }

        try {
            manager.registerCarVolumeCallback(callback)
            volumeCallback = callback
            Log.d(TAG, "Registered CarVolumeCallback")
        } catch (e: Exception) {
            // SecurityException here means the platform gates even observation.
            Log.d(TAG, "registerCarVolumeCallback rejected: ${e::class.java.simpleName}: ${e.message}")
        }
    }

    fun release() {
        volumeCallback?.let {
            try {
                carAudioManager?.unregisterCarVolumeCallback(it)
            } catch (e: Exception) {
                Log.d(TAG, "unregisterCarVolumeCallback failed: ${e.message}")
            }
        }
        volumeCallback = null
        try {
            car?.disconnect()
        } catch (e: Exception) {
            Log.d(TAG, "disconnect failed: ${e.message}")
        }
        car = null
        carAudioManager = null
    }
}
