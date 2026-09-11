package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
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
        viewModel.speed.observe(this) { if (it != lastSpeed) { lastSpeed = it; invalidate() } }
        viewModel.rpm.observe(this) { if (it != lastRpm) { lastRpm = it; invalidate() } }
        viewModel.fuel.observe(this) { if (it != lastFuel) { lastFuel = it; invalidate() } }
        viewModel.gear.observe(this) { if (it != lastGear) { lastGear = it ?: ""; invalidate() } }
        viewModel.engineOn.observe(this) { invalidate() }
        viewModel.isConnected.observe(this) { invalidate() }
        viewModel.currentAlert.observe(this) { invalidate() }
        viewModel.odometer.observe(this) { invalidate() }
    }

    override fun onGetTemplate(): Template {
        if (viewModel.isConnected.value != true) {
            return ListTemplate.Builder()
                .setTitle("Vehicle Dashboard")
                .setHeaderAction(Action.BACK)
                .addSectionedList(
                    SectionedItemList.create(
                        ItemList.Builder()
                            .addItem(
                                Row.Builder()
                                    .setTitle("Connecting to Vehicle Service")
                                    .addText("Please wait…")
                                    .build()
                            )
                            .build(),
                        "Status"
                    )
                )
                .build()
        }

        val speed     = viewModel.speed.value ?: 0f
        val rpm       = viewModel.rpm.value ?: 0f
        val fuel      = viewModel.fuel.value ?: 0f
        val gear      = viewModel.gear.value ?: "P"
        val engineOn  = viewModel.engineOn.value ?: false
        val odometer  = viewModel.odometer.value ?: 0f
        val battery   = 80f // battery level not exposed on emulator VHAL
        val hasAlert  = viewModel.currentAlert.value != null
        val rangeKm   = (56f * (fuel / 100f)).toInt() * 10

        return ListTemplate.Builder()
            .setTitle("Vehicle Dashboard")
            .setHeaderAction(Action.BACK)
            .addSectionedList(
                SectionedItemList.create(
                    ItemList.Builder()
                        .addItem(
                            Row.Builder()
                                .setTitle("Speed  ·  ${speed.toInt()} km/h  ·  Gear $gear")
                                .addText(engineStatusText(hasAlert))
                                .setImage(CarIcon.Builder(IconCompat.createWithBitmap(
                                    GaugeDrawer.drawSpeedDial(speed)
                                )).build())
                                .build()
                        )
                        .build(),
                    "Speed"
                )
            )
            .addSectionedList(
                SectionedItemList.create(
                    ItemList.Builder()
                        .addItem(
                            Row.Builder()
                                .setTitle("Engine RPM  ·  ${rpm.toInt()}")
                                .setImage(CarIcon.Builder(IconCompat.createWithBitmap(
                                    GaugeDrawer.drawRpmArc(rpm)
                                )).build())
                                .build()
                        )
                        .addItem(
                            Row.Builder()
                                .setTitle("Transmission & Engine")
                                .setImage(CarIcon.Builder(IconCompat.createWithBitmap(
                                    GaugeDrawer.drawGearStrip(gear, engineOn, hasAlert)
                                )).build())
                                .build()
                        )
                        .build(),
                    "Engine"
                )
            )
            .addSectionedList(
                SectionedItemList.create(
                    ItemList.Builder()
                        .addItem(
                            Row.Builder()
                                .setTitle("Fuel Level")
                                .addText("Estimated range ≈ ${rangeKm} km")
                                .setImage(CarIcon.Builder(IconCompat.createWithBitmap(
                                    GaugeDrawer.drawFuelBar(fuel)
                                )).build())
                                .build()
                        )
                        .addItem(
                            Row.Builder()
                                .setTitle("Battery (simulated)")
                                .setImage(CarIcon.Builder(IconCompat.createWithBitmap(
                                    GaugeDrawer.drawBatteryBar(battery)
                                )).build())
                                .build()
                        )
                        .build(),
                    "Energy"
                )
            )
            .addSectionedList(
                SectionedItemList.create(
                    ItemList.Builder()
                        .addItem(
                            Row.Builder()
                                .setTitle("Odometer")
                                .addText(String.format("Total: %.1f km", odometer))
                                .addText(if (engineOn) "Engine ON" else "Engine OFF")
                                .build()
                        )
                        .apply {
                            if (hasAlert) {
                                val alert = viewModel.currentAlert.value
                                if (alert != null) {
                                    addItem(
                                        Row.Builder()
                                            .setTitle("Active Alert: ${alert.message}")
                                            .addText("Severity: ${alert.severity.name}")
                                            .build()
                                    )
                                }
                            }
                        }
                        .build(),
                    "Vehicle"
                )
            )
            .build()
    }

    private fun engineStatusText(hasAlert: Boolean): String {
        return if (hasAlert) "⚠ Check Required" else "● All Systems Normal"
    }
}