package com.swapnil.smart.aaos.vehicle

import android.content.*
import android.os.IBinder
import android.util.Log

object VehicleRepository {

    private const val TAG = "VehicleRepository"

    private var vehicleService: IVehicleDataService? = null
    var isConnected: Boolean = false
        private set

    private var isBinding = false

    // ✅ Service connection (ONLY ONE in whole app)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "✅ AIDL connected (GLOBAL)")
            vehicleService = IVehicleDataService.Stub.asInterface(service)
            isConnected = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "❌ AIDL disconnected")
            vehicleService = null
            isConnected = false
        }
    }

    // ✅ Call this ONCE (from first screen)
    fun connect(context: Context) {
        if (isConnected || isBinding) return

        try {
            val intent = Intent(context, VehicleDataService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            isBinding = true
            Log.d(TAG, "🔄 Binding to VehicleDataService...")
        } catch (e: Exception) {
            Log.d(TAG, "Bind failed: ${e.message}")
        }
    }

    // ✅ Access methods (safe calls)

    fun getSpeed(): Float {
        return try { vehicleService?.getSpeed() ?: 0f } catch (e: Exception) { 0f }
    }

    fun getRpm(): Float {
        return try { vehicleService?.getRpm() ?: 0f } catch (e: Exception) { 0f }
    }

    fun getFuel(): Float {
        return try { vehicleService?.getFuelLevel() ?: 0f } catch (e: Exception) { 0f }
    }

    fun getGear(): String {
        return try { vehicleService?.getGear() ?: "P" } catch (e: Exception) { "P" }
    }

    fun isEngineOn(): Boolean {
        return try { vehicleService?.isEngineOn() ?: false } catch (e: Exception) { false }
    }

    fun getOdometer(): Float {
        return try { vehicleService?.getOdometer() ?: 0f } catch (e: Exception) { 0f }
    }

    fun simulateDriving() {
        try {
            vehicleService?.simulateDriving(60f, 2000f, 70f)
        } catch (e: Exception) {
            Log.d(TAG, "Simulate driving error: ${e.message}")
        }
    }

    fun simulateParked() {
        try {
            vehicleService?.simulateParked()
        } catch (e: Exception) {
            Log.d(TAG, "Simulate parked error: ${e.message}")
        }
    }
}