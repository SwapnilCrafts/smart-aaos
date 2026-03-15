package com.swapnil.smart.aaos.screens

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
import com.swapnil.smart.aaos.VehicleData
import com.swapnil.smart.aaos.VehicleDataManager

class DashboardScreen(carContext: CarContext) : Screen(carContext) {

    private var vehicleData = VehicleData()
    private var isSimulating = false

    init {
        // ✅ Initialize vehicle data
        VehicleDataManager.init(carContext) { data ->
            vehicleData = data
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {

        val listBuilder = ItemList.Builder()

        // ✅ Speed row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🚗 Speed")
                .addText("${vehicleData.speedKmh.toInt()} km/h")
                .addText(getSpeedStatus(vehicleData.speedKmh))
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            android.R.drawable.ic_menu_compass
                        )
                    ).setTint(getSpeedColor(vehicleData.speedKmh)).build()
                )
                .build()
        )

        // ✅ RPM row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("⚙️ Engine RPM")
                .addText("${vehicleData.rpm.toInt()} RPM")
                .addText(getRpmStatus(vehicleData.rpm))
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            android.R.drawable.ic_menu_rotate
                        )
                    ).setTint(getRpmColor(vehicleData.rpm)).build()
                )
                .build()
        )

        // ✅ Fuel row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("⛽ Fuel Level")
                .addText("${vehicleData.fuelPercent.toInt()}%")
                .addText(getFuelStatus(vehicleData.fuelPercent))
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            android.R.drawable.ic_menu_gallery
                        )
                    ).setTint(getFuelColor(vehicleData.fuelPercent)).build()
                )
                .build()
        )

        // ✅ Gear row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🔧 Gear")
                .addText("${vehicleData.gear} — ${getGearName(vehicleData.gear)}")
                .addText(
                    if (vehicleData.isEngineOn) "Engine ON 🟢"
                    else "Engine OFF 🔴"
                )
                .build()
        )

        // ✅ Odometer row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("📍 Odometer")
                .addText("${String.format("%.1f", vehicleData.odometerKm)} km")
                .addText("Total distance driven")
                .build()
        )

        return ListTemplate.Builder()
            .setTitle("🚗 Vehicle Dashboard")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle(if (isSimulating) "🔴 Stop" else "▶ Simulate")
                            .setOnClickListener {
                                if (isSimulating) {
                                    // Stop simulation
                                    isSimulating = false
                                    VehicleDataManager.simulateData()
                                } else {
                                    // Start simulation — car driving
                                    isSimulating = true
                                    VehicleDataManager.simulateData(
                                        speedKmh = 65f,
                                        rpm = 2500f,
                                        fuelPercent = 75f,
                                        gear = "D",
                                        isEngineOn = true,
                                        odometerKm = 12450f
                                    )
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

    // ✅ Helper functions for status text
    private fun getSpeedStatus(speed: Float): String {
        return when {
            speed == 0f -> "Parked"
            speed < 30f -> "Slow speed"
            speed < 60f -> "City driving"
            speed < 100f -> "Highway"
            else -> "High speed!"
        }
    }

    private fun getRpmStatus(rpm: Float): String {
        return when {
            rpm == 0f -> "Engine off"
            rpm < 1000f -> "Idle"
            rpm < 3000f -> "Normal"
            rpm < 5000f -> "High"
            else -> "Redline!"
        }
    }

    private fun getFuelStatus(fuel: Float): String {
        return when {
            fuel < 10f -> "⚠️ Low fuel!"
            fuel < 25f -> "Fill up soon"
            fuel < 50f -> "Half tank"
            else -> "Good"
        }
    }

    private fun getGearName(gear: String): String {
        return when (gear) {
            "P" -> "Park"
            "R" -> "Reverse"
            "N" -> "Neutral"
            "D" -> "Drive"
            else -> "Unknown"
        }
    }

    // ✅ Color coding
    private fun getSpeedColor(speed: Float): CarColor {
        return when {
            speed == 0f -> CarColor.DEFAULT
            speed < 80f -> CarColor.GREEN
            speed < 120f -> CarColor.YELLOW
            else -> CarColor.RED
        }
    }

    private fun getRpmColor(rpm: Float): CarColor {
        return when {
            rpm < 3000f -> CarColor.GREEN
            rpm < 5000f -> CarColor.YELLOW
            else -> CarColor.RED
        }
    }

    private fun getFuelColor(fuel: Float): CarColor {
        return when {
            fuel < 10f -> CarColor.RED
            fuel < 25f -> CarColor.YELLOW
            else -> CarColor.GREEN
        }
    }
}