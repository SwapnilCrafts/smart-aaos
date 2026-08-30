package com.swapnil.smart.aaos.vehicle

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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

    // ✅ AIDL Stub — implements the interface
    private val binder = object : IVehicleDataService.Stub() {

        override fun getSpeed(): Float {
            val vhal = halManager.getSpeedKmh()
            if (vhal != null) {
                Log.d(TAG, "getSpeed (VHAL): $vhal km/h")
                return vhal
            }
            Log.d(TAG, "getSpeed (simulated): $currentSpeed km/h")
            return currentSpeed
        }

        override fun getRpm(): Float {
            val vhal = halManager.getRpm()
            if (vhal != null) {
                Log.d(TAG, "getRpm (VHAL): $vhal RPM")
                return vhal
            }
            Log.d(TAG, "getRpm (simulated): $currentRpm RPM")
            return currentRpm
        }

        override fun getFuelLevel(): Float {
            val vhal = halManager.getFuelLevelFraction()
            if (vhal != null) {
                Log.d(TAG, "getFuelLevel (VHAL): $vhal fraction")
                return vhal * 100f
            }
            Log.d(TAG, "getFuelLevel (simulated): $currentFuel%")
            return currentFuel
        }

        override fun getGear(): String {
            val vhal = halManager.getGearString()
            if (vhal != null) {
                Log.d(TAG, "getGear (VHAL): $vhal")
                return vhal
            }
            Log.d(TAG, "getGear (simulated): $currentGear")
            return currentGear
        }

        override fun isEngineOn(): Boolean {
            val vhal = halManager.isEngineOn()
            if (vhal != null) {
                Log.d(TAG, "isEngineOn (VHAL): $vhal")
                return vhal
            }
            Log.d(TAG, "isEngineOn (simulated): $engineOn")
            return engineOn
        }

        override fun getOdometer(): Float {
            val vhal = halManager.getOdometerKm()
            if (vhal != null) {
                Log.d(TAG, "getOdometer (VHAL): $vhal km")
                return vhal
            }
            Log.d(TAG, "getOdometer (simulated): $currentOdometer km")
            return currentOdometer
        }

        override fun getMake(): String {
            val vhal = halManager.getMake()
            if (vhal != null) {
                Log.d(TAG, "getMake (VHAL): $vhal")
                return vhal
            }
            Log.d(TAG, "getMake (simulated): Smart AAOS EV")
            return "Smart AAOS EV"
        }

        override fun getModel(): String {
            val vhal = halManager.getModel()
            if (vhal != null) {
                Log.d(TAG, "getModel (VHAL): $vhal")
                return vhal
            }
            Log.d(TAG, "getModel (simulated): Twin Turbo")
            return "Twin Turbo"
        }

        override fun getVin(): String {
            val vhal = halManager.getVin()
            if (vhal != null) {
                Log.d(TAG, "getVin (VHAL): $vhal")
                return vhal
            }
            Log.d(TAG, "getVin (restricted): signature-level permission")
            return ""
        }

        override fun getModelYear(): Int {
            val vhal = halManager.getModelYear()
            if (vhal != null) {
                Log.d(TAG, "getModelYear (VHAL): $vhal")
                return vhal
            }
            Log.d(TAG, "getModelYear (restricted): signature-level permission")
            return 0
        }

        override fun getFuelCapacityLitres(): Float {
            val vhal = halManager.getFuelCapacityLitres()
            if (vhal != null) {
                Log.d(TAG, "getFuelCapacityLitres (VHAL): $vhal L")
                return vhal
            }
            Log.d(TAG, "getFuelCapacityLitres (restricted): signature-level permission")
            return -1f
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
        }

        override fun simulateParked() {
            Log.d(TAG, "simulateParked called")
            currentSpeed = 0f
            currentRpm = 800f
            currentGear = "P"
            engineOn = true
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VehicleDataService created")
        halManager = VehicleHalManager(this)
        // One-shot self-check: prove the CAR_INFO-gated INFO properties read
        // REAL VHAL values (not simulated). Runs after the car connection settles.
        val h = Handler(Looper.getMainLooper())
        val check = object : Runnable {
            var attempt = 0
            override fun run() {
                attempt++
                Log.d(TAG, "INFO self-check #$attempt → Make=${halManager.getMake()} Model=${halManager.getModel()} " +
                        "Year=${halManager.getModelYear()} VIN=${halManager.getVin()} " +
                        "FuelCap=${halManager.getFuelCapacityLitres()}L")
                if (attempt < 5) h.postDelayed(this, 2000)
            }
        }
        h.postDelayed(check, 3000)
    }

    // ✅ Return binder to clients
    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "Client connected to VehicleDataService")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        halManager.release()
        Log.d(TAG, "VehicleDataService destroyed")
    }
}