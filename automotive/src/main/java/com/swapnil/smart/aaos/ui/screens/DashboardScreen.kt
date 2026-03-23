package com.swapnil.smart.aaos.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.vehicle.IVehicleDataService
import com.swapnil.smart.aaos.vehicle.VehicleDataService

class DashboardScreen(carContext: CarContext) : Screen(carContext) {

    companion object {
        private const val TAG = "SmartAAOS_AIDL"
    }

    // ✅ AIDL service reference
    private var vehicleService: IVehicleDataService? = null
    private var isSimulating = false
    private var isConnected = false

    // ✅ Auto refresh handler
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (isConnected) invalidate()
            handler.postDelayed(this, 1000L)
        }
    }

    // ✅ AIDL Service Connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "✅ AIDL Service connected!")
            vehicleService = IVehicleDataService.Stub.asInterface(service)
            isConnected = true
            invalidate()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "❌ AIDL Service disconnected!")
            vehicleService = null
            isConnected = false
            invalidate()
        }
    }

    init {
        // ✅ Connect to AIDL VehicleDataService
        try {
            val intent = Intent(carContext, VehicleDataService::class.java)
            carContext.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
            Log.d(TAG, "Connecting to VehicleDataService via AIDL...")
        } catch (e: Exception) {
            Log.d(TAG, "Failed to bind: ${e.message}")
        }

        // ✅ Start auto refresh
        handler.post(refreshRunnable)
    }

    override fun onGetTemplate(): Template {

        val listBuilder = ItemList.Builder()

        if (!isConnected) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🔄 Connecting to Vehicle Service...")
                    .addText("Please wait")
                    .addText("Initializing AIDL connection")
                    .build()
            )
        } else {
            // ✅ Read ALL data via AIDL
            val speed = try { vehicleService?.getSpeed() ?: 0f } catch (e: Exception) { 0f }
            val rpm = try { vehicleService?.getRpm() ?: 0f } catch (e: Exception) { 0f }
            val fuel = try { vehicleService?.getFuelLevel() ?: 0f } catch (e: Exception) { 0f }
            val gear = try { vehicleService?.getGear() ?: "P" } catch (e: Exception) { "P" }
            val engineOn = try { vehicleService?.isEngineOn() ?: false } catch (e: Exception) { false }
            val odometer = try { vehicleService?.getOdometer() ?: 0f } catch (e: Exception) { 0f }

            Log.d(TAG, "AIDL data: speed=$speed rpm=$rpm fuel=$fuel gear=$gear")

            // Speed row
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🚗 Speed")
                    .addText("${speed.toInt()} km/h")
                    .addText(getSpeedStatus(speed))
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext,
                                android.R.drawable.ic_menu_compass
                            )
                        ).setTint(getSpeedColor(speed)).build()
                    )
                    .build()
            )

            // RPM row
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⚙️ Engine RPM")
                    .addText("${rpm.toInt()} RPM")
                    .addText(getRpmStatus(rpm))
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext,
                                android.R.drawable.ic_menu_rotate
                            )
                        ).setTint(getRpmColor(rpm)).build()
                    )
                    .build()
            )

            // Fuel row
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⛽ Fuel Level")
                    .addText("${fuel.toInt()}%")
                    .addText(getFuelStatus(fuel))
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext,
                                android.R.drawable.ic_menu_gallery
                            )
                        ).setTint(getFuelColor(fuel)).build()
                    )
                    .build()
            )

            // Gear row
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🔧 Gear & Engine")
                    .addText("$gear — ${getGearName(gear)}")
                    .addText(if (engineOn) "Engine ON 🟢" else "Engine OFF 🔴")
                    .build()
            )

            // Odometer row
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("📍 Odometer")
                    .addText("${String.format("%.1f", odometer)} km")
                    .addText("Via AIDL Service ✅")
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle(
                if (isConnected) "🚗 Dashboard — AIDL ✅"
                else "🚗 Dashboard — Connecting..."
            )
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle(
                                if (isSimulating) "🅿️ Park"
                                else "🚗 Drive"
                            )
                            .setOnClickListener {
                                try {
                                    if (isSimulating) {
                                        // ✅ Call AIDL simulateParked
                                        vehicleService?.simulateParked()
                                        isSimulating = false
                                    } else {
                                        // ✅ Call AIDL simulateDriving
                                        vehicleService?.simulateDriving(65f, 2500f, 75f)
                                        isSimulating = true
                                    }
                                } catch (e: Exception) {
                                    Log.d(TAG, "AIDL call failed: ${e.message}")
                                }
                                invalidate()
                            }
                            .build()
                    )
                    .build()
            )
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun getSpeedStatus(speed: Float) = when {
        speed == 0f -> "Parked"
        speed < 30f -> "Slow speed"
        speed < 60f -> "City driving"
        speed < 100f -> "Highway"
        else -> "High speed!"
    }

    private fun getRpmStatus(rpm: Float) = when {
        rpm == 0f -> "Engine off"
        rpm < 1000f -> "Idle"
        rpm < 3000f -> "Normal"
        rpm < 5000f -> "High"
        else -> "Redline!"
    }

    private fun getFuelStatus(fuel: Float) = when {
        fuel < 10f -> "⚠️ Low fuel!"
        fuel < 25f -> "Fill up soon"
        fuel < 50f -> "Half tank"
        else -> "Good"
    }

    private fun getGearName(gear: String) = when (gear) {
        "P" -> "Park"
        "R" -> "Reverse"
        "N" -> "Neutral"
        "D" -> "Drive"
        else -> "Unknown"
    }

    private fun getSpeedColor(speed: Float) = when {
        speed == 0f -> CarColor.DEFAULT
        speed < 80f -> CarColor.GREEN
        speed < 120f -> CarColor.YELLOW
        else -> CarColor.RED
    }

    private fun getRpmColor(rpm: Float) = when {
        rpm < 3000f -> CarColor.GREEN
        rpm < 5000f -> CarColor.YELLOW
        else -> CarColor.RED
    }

    private fun getFuelColor(fuel: Float) = when {
        fuel < 10f -> CarColor.RED
        fuel < 25f -> CarColor.YELLOW
        else -> CarColor.GREEN
    }
}