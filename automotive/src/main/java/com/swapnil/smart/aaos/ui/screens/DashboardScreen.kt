package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.ui.GaugeDrawer
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel

class DashboardScreen(carContext: CarContext) : Screen(carContext) {

    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    private var lastSpeed = Float.MIN_VALUE
    private var lastRpm = Float.MIN_VALUE
    private var lastFuel = Float.MIN_VALUE
    private var lastGear = ""
    private var lastBattery = Float.MIN_VALUE

    init {
        // Invalidate only on actual value change (avoids constant host rebuilds
        // that reset list scroll). The gauges re-render when a value moves.
        viewModel.speed.observeForever { if (it != lastSpeed) { lastSpeed = it; invalidate() } }
        viewModel.rpm.observeForever { if (it != lastRpm) { lastRpm = it; invalidate() } }
        viewModel.fuel.observeForever { if (it != lastFuel) { lastFuel = it; invalidate() } }
        viewModel.gear.observeForever { if (it != lastGear) { lastGear = it ?: ""; invalidate() } }
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
            val speed     = viewModel.speed.value ?: 0f
            val rpm       = viewModel.rpm.value ?: 0f
            val fuel      = viewModel.fuel.value ?: 0f
            val gear      = viewModel.gear.value ?: "P"
            val engineOn  = viewModel.engineOn.value ?: false
            val odometer  = viewModel.odometer.value ?: 0f
            val battery   = 80f // battery level not exposed on emulator VHAL
            val hasAlert  = viewModel.currentAlert.value != null

            // Big speed dial
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Speed  ·  ${speed.toInt()} km/h  ·  Gear $gear")
                    .addText(engineStatusText(hasAlert))
                    .setImage(CarIcon.Builder(IconCompat.createWithBitmap(
                        GaugeDrawer.drawSpeedDial(speed)
                    )).build())
                    .build()
            )

            // RPM arc
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Engine RPM  ·  ${rpm.toInt()}")
                    .setImage(CarIcon.Builder(IconCompat.createWithBitmap(
                        GaugeDrawer.drawRpmArc(rpm)
                    )).build())
                    .build()
            )

            // Fuel bar
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Fuel Level")
                    .setImage(CarIcon.Builder(IconCompat.createWithBitmap(
                        GaugeDrawer.drawFuelBar(fuel)
                    )).build())
                    .build()
            )

            // Battery bar
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Battery (simulated)")
                    .setImage(CarIcon.Builder(IconCompat.createWithBitmap(
                        GaugeDrawer.drawBatteryBar(battery)
                    )).build())
                    .build()
            )

            // Odometer
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Odometer")
                    .addText(String.format("Total: %.1f km", odometer))
                    .addText(if (engineOn) "Engine ON" else "Engine OFF")
                    .build()
            )

            viewModel.currentAlert.value?.let { alert ->
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle("Active Alert: ${alert.message}")
                        .addText("Severity: ${alert.severity.name}")
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle("Vehicle Dashboard")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun engineStatusText(hasAlert: Boolean): String {
        return if (hasAlert) "⚠ Check Required" else "● All Systems Normal"
    }
}
