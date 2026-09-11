package com.swapnil.smart.aaos.vehicle

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import com.swapnil.smart.aaos.vehicle.IVehicleDataService


class VehicleDataService : Service() {

    companion object {
        private const val TAG = "SmartAAOS_AIDL"
    }

    // ✅ Fallback simulated vehicle data (used only when VHAL is unavailable)
    private var currentSpeed = 0f
    private var currentRpm = 800f
    private var currentFuel = 75f
    private var currentGear = "P"
    private var engineOn = true
    private var currentOdometer = 12450f

    private lateinit var halManager: VehicleHalManager

    /**
     * Registered clients. RemoteCallbackList handles the hard parts: it holds
     * weak death recipients, so a client that crashes is dropped
     * automatically rather than leaking a Binder proxy.
     */
    private val callbacks = RemoteCallbackList<IVehicleDataCallback>()

    /** Last message logged per key, so a 1 Hz poll cannot flood logcat. */
    private val lastLogged = HashMap<String, String>()

    /** Logs [message] only when it differs from the previous one for [key]. */
    private fun logOnChange(key: String, message: String) {
        if (lastLogged.put(key, message) != message) Log.d(TAG, message)
    }

    // ✅ AIDL Stub — implements the interface
    private val binder = object : IVehicleDataService.Stub() {

        override fun getSpeed(): Float {
            val vhal = halManager.getSpeedKmh()
            if (vhal != null) {
                logOnChange("getSpeed", "getSpeed (VHAL): $vhal km/h")
                return vhal
            }
            logOnChange("getSpeed", "getSpeed (simulated): $currentSpeed km/h")
            return currentSpeed
        }

        override fun getRpm(): Float {
            val vhal = halManager.getRpm()
            if (vhal != null) {
                logOnChange("getRpm", "getRpm (VHAL): $vhal RPM")
                return vhal
            }
            logOnChange("getRpm", "getRpm (simulated): $currentRpm RPM")
            return currentRpm
        }

        override fun getFuelLevel(): Float {
            val vhal = halManager.getFuelLevelFraction()
            if (vhal != null) {
                logOnChange("getFuelLevel", "getFuelLevel (VHAL): $vhal fraction")
                return vhal * 100f
            }
            logOnChange("getFuelLevel", "getFuelLevel (simulated): $currentFuel%")
            return currentFuel
        }

        override fun getGear(): String {
            val vhal = halManager.getGearString()
            if (vhal != null) {
                logOnChange("getGear", "getGear (VHAL): $vhal")
                return vhal
            }
            logOnChange("getGear", "getGear (simulated): $currentGear")
            return currentGear
        }

        override fun isEngineOn(): Boolean {
            val vhal = halManager.isEngineOn()
            if (vhal != null) {
                logOnChange("isEngineOn", "isEngineOn (VHAL): $vhal")
                return vhal
            }
            logOnChange("isEngineOn", "isEngineOn (simulated): $engineOn")
            return engineOn
        }

        override fun getOdometer(): Float {
            val vhal = halManager.getOdometerKm()
            if (vhal != null) {
                logOnChange("getOdometer", "getOdometer (VHAL): $vhal km")
                return vhal
            }
            logOnChange("getOdometer", "getOdometer (simulated): $currentOdometer km")
            return currentOdometer
        }

        override fun getMake(): String {
            val vhal = halManager.getMake()
            if (vhal != null) {
                logOnChange("getMake", "getMake (VHAL): $vhal")
                return vhal
            }
            logOnChange("getMake", "getMake (simulated): Smart AAOS EV")
            return "Smart AAOS EV"
        }

        override fun getModel(): String {
            val vhal = halManager.getModel()
            if (vhal != null) {
                logOnChange("getModel", "getModel (VHAL): $vhal")
                return vhal
            }
            logOnChange("getModel", "getModel (simulated): Twin Turbo")
            return "Twin Turbo"
        }

        override fun getVin(): String {
            val vhal = halManager.getVin()
            if (vhal != null) {
                logOnChange("getVin", "getVin (VHAL): $vhal")
                return vhal
            }
            logOnChange("getVin", "getVin (restricted): signature-level permission")
            return ""
        }

        override fun getModelYear(): Int {
            val vhal = halManager.getModelYear()
            if (vhal != null) {
                logOnChange("getModelYear", "getModelYear (VHAL): $vhal")
                return vhal
            }
            logOnChange("getModelYear", "getModelYear (restricted): signature-level permission")
            return 0
        }

        override fun getFuelCapacityLitres(): Float {
            val vhal = halManager.getFuelCapacityLitres()
            if (vhal != null) {
                logOnChange("getFuelCapacityLitres", "getFuelCapacityLitres (VHAL): $vhal L")
                return vhal
            }
            logOnChange("getFuelCapacityLitres", "getFuelCapacityLitres (restricted): signature-level permission")
            return -1f
        }

        override fun registerCallback(callback: IVehicleDataCallback?) {
            if (callback == null) return
            callbacks.register(callback)
            Log.d(TAG, "Client registered for push updates")
            // Send current state straight away so the client never polls for
            // its initial values.
            try {
                pushTo(callback)
            } catch (e: RemoteException) {
                Log.d(TAG, "Initial push failed: ${e.message}")
            }
        }

        override fun unregisterCallback(callback: IVehicleDataCallback?) {
            if (callback == null) return
            callbacks.unregister(callback)
            Log.d(TAG, "Client unregistered")
        }

        override fun simulateDriving(
            speedKmh: Float,
            rpm: Float,
            fuel: Float
        ) {
            Log.d(TAG, "simulateDriving: speed=$speedKmh rpm=$rpm fuel=$fuel")
            currentSpeed = speedKmh
            currentRpm = rpm
            currentFuel = fuel
            currentGear = "D"
            engineOn = true
            currentOdometer += 0.1f
            broadcast()
        }

        override fun simulateParked() {
            Log.d(TAG, "simulateParked called")
            currentSpeed = 0f
            currentRpm = 800f
            currentGear = "P"
            engineOn = true
            broadcast()
        }
    }

    /** Pushes current state to one client. */
    private fun pushTo(callback: IVehicleDataCallback) {
        callback.onVehicleData(
            binder.speed,
            binder.rpm,
            binder.fuelLevel,
            binder.gear,
            binder.isEngineOn,
            binder.odometer
        )
    }

    /**
     * Fans current state out to every registered client. Called from the VHAL
     * event callback and whenever a simulated value changes - never on a
     * timer, so an idle vehicle produces no traffic at all.
     */
    private fun broadcast() {
        val n = callbacks.beginBroadcast()
        try {
            for (i in 0 until n) {
                try {
                    pushTo(callbacks.getBroadcastItem(i))
                } catch (e: RemoteException) {
                    // Client died; RemoteCallbackList will evict it.
                    Log.d(TAG, "Broadcast to a client failed: ${e.message}")
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VehicleDataService created")
        halManager = VehicleHalManager(this)

        // Subscribe to the VHAL and forward each event to our clients, rather
        // than having them poll us and us poll getProperty().
        halManager.startSubscriptions { broadcast() }
        // The car service connection settles a moment after process start, so
        // give it a beat before reporting which properties are genuinely live.
        // Read it with: adb logcat -s SmartAAOS_AIDL:D SmartAAOS_VHAL:D
        Handler(Looper.getMainLooper()).postDelayed({
            halManager.logAvailability()
            Log.d(TAG, "INFO self-check -> Make=${halManager.getMake()} Model=${halManager.getModel()} " +
                    "Year=${halManager.getModelYear()} VIN=${halManager.getVin()} " +
                    "FuelCap=${halManager.getFuelCapacityLitres()}L")
        }, 3000)
    }

    // ✅ Return binder to clients
    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "Client connected to VehicleDataService")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        callbacks.kill()
        halManager.release()
        Log.d(TAG, "VehicleDataService destroyed")
    }
}