package com.swapnil.smart.aaos.vehicle

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.swapnil.smart.aaos.vehicle.IVehicleDataService


class VehicleDataService : Service() {

    companion object {
        private const val TAG = "SmartAAOS_AIDL"
    }

    // ✅ Current vehicle data
    private var currentSpeed = 0f
    private var currentRpm = 800f
    private var currentFuel = 75f
    private var currentGear = "P"
    private var engineOn = true
    private var currentOdometer = 12450f

    // ✅ AIDL Stub — implements the interface
    private val binder = object : IVehicleDataService.Stub() {

        override fun getSpeed(): Float {
            Log.d(TAG, "getSpeed called: $currentSpeed km/h")
            return currentSpeed
        }

        override fun getRpm(): Float {
            Log.d(TAG, "getRpm called: $currentRpm RPM")
            return currentRpm
        }

        override fun getFuelLevel(): Float {
            Log.d(TAG, "getFuelLevel called: $currentFuel%")
            return currentFuel
        }

        override fun getGear(): String {
            Log.d(TAG, "getGear called: $currentGear")
            return currentGear
        }

        override fun isEngineOn(): Boolean {
            Log.d(TAG, "isEngineOn called: $engineOn")
            return engineOn
        }

        override fun getOdometer(): Float {
            Log.d(TAG, "getOdometer called: $currentOdometer km")
            return currentOdometer
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
    }

    // ✅ Return binder to clients
    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "Client connected to VehicleDataService")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VehicleDataService destroyed")
    }
}