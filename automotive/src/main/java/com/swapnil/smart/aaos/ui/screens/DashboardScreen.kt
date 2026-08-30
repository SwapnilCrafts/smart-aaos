package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel

class DashboardScreen(carContext: CarContext) : Screen(carContext) {

    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    init {
        viewModel.speed.observeForever { invalidate() }
        viewModel.rpm.observeForever { invalidate() }
        viewModel.fuel.observeForever { invalidate() }
        viewModel.gear.observeForever { invalidate() }
        viewModel.engineOn.observeForever { invalidate() }
        viewModel.isConnected.observeForever { invalidate() }
        viewModel.currentAlert.observeForever { invalidate() }
        viewModel.odometer.observeForever { invalidate() }
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        if (viewModel.isConnected.value != true) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Connecting to Vehicle Service")
                    .addText("Please wait…")
                    .build()
            )
        } else {
            val speed    = viewModel.speed.value ?: 0f
            val rpm      = viewModel.rpm.value ?: 0f
            val fuel     = viewModel.fuel.value ?: 0f
            val gear     = viewModel.gear.value ?: "P"
            val engineOn = viewModel.engineOn.value ?: false
            val odometer = viewModel.odometer.value ?: 0f
            val hasAlert = viewModel.currentAlert.value != null

            // Row 1 — Vehicle status summary
            val healthStatus = if (hasAlert) "Check Required" else "All Systems Normal"
            val engineState  = if (engineOn) "Engine ON" else "Engine OFF"
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Vehicle Status")
                    .addText(healthStatus)
                    .addText("$engineState  ·  Gear: $gear")
                    .build()
            )

            // Row 2 — Speed & Engine
            val speedStatus = when {
                speed > 100 -> "Overspeed"
                speed > 60  -> "High Speed"
                speed > 0   -> "Normal"
                else        -> "Stationary"
            }
            val rpmStatus = when {
                rpm > 5000  -> "High RPM"
                rpm > 3500  -> "Elevated"
                rpm > 0     -> "Normal"
                else        -> "Idle"
            }
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Speed & Engine")
                    .addText("${speed.toInt()} km/h  ·  ${rpm.toInt()} RPM")
                    .addText("Speed: $speedStatus  ·  Engine: $rpmStatus")
                    .build()
            )

            // Row 3 — Fuel & Odometer
            val fuelStatus = when {
                fuel < 10  -> "Critical — Refuel Now"
                fuel < 25  -> "Low — Refuel Soon"
                fuel < 50  -> "Moderate"
                else       -> "Good"
            }
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Fuel & Odometer")
                    .addText("Fuel: ${fuel.toInt()}%  ($fuelStatus)")
                    .addText("Odometer: ${String.format("%.1f", odometer)} km")
                    .build()
            )

            // Row 4 — Active alert (only when present)
            viewModel.currentAlert.value?.let { alert ->
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle("Active Alert: ${alert.message}")
                        .addText("Severity: ${alert.severity.name}  ·  Tap Diagnostics for details")
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle("Vehicle Dashboard")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle(
                                if ((viewModel.speed.value ?: 0f) > 2f) "Park" else "Drive"
                            )
                            .setOnClickListener {
                                if ((viewModel.speed.value ?: 0f) > 2f) viewModel.simulateParked()
                                else viewModel.simulateDriving()
                            }
                            .build()
                    )
                    .build()
            )
            .setSingleList(listBuilder.build())
            .build()
    }
}
