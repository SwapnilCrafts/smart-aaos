package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel
import com.swapnil.smart.aaos.vehicle.VehicleRepository
import com.swapnil.smart.aaos.utils.VehicleAlert
import java.util.Locale

class DiagnosticsScreen(carContext: CarContext) : Screen(carContext) {

    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    private var lastAlert: VehicleAlert? = null
    private var lastSpeed = Float.MIN_VALUE
    private var lastRpm = Float.MIN_VALUE
    private var lastFuel = Float.MIN_VALUE

    init {
        // Invalidate only on ACTUAL value change. The VehicleViewModel polls
        // every second; invalidating on every tick makes the host rebuild the
        // scrollable ListTemplate each second, which resets scroll to the top.
        viewModel.currentAlert.observeForever {
            if (it != lastAlert) {
                lastAlert = it
                invalidate()
            }
        }
        viewModel.speed.observeForever {
            if (it != lastSpeed) {
                lastSpeed = it
                invalidate()
            }
        }
        viewModel.rpm.observeForever {
            if (it != lastRpm) {
                lastRpm = it
                invalidate()
            }
        }
        viewModel.fuel.observeForever {
            if (it != lastFuel) {
                lastFuel = it
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val speed = viewModel.speed.value ?: 0f
        val rpm   = viewModel.rpm.value ?: 0f
        val fuel  = viewModel.fuel.value ?: 0f

        // ── Vehicle Info section (real VHAL data via CAR_INFO permission) ──
        val infoListBuilder = ItemList.Builder()
        infoListBuilder.addItem(
            Row.Builder()
                .setTitle("Make")
                .addText(VehicleRepository.getMake())
                .build()
        )
        infoListBuilder.addItem(
            Row.Builder()
                .setTitle("Model")
                .addText(VehicleRepository.getModel())
                .build()
        )
        infoListBuilder.addItem(
            Row.Builder()
                .setTitle("Model Year")
                .addText(VehicleRepository.getModelYear().takeIf { it > 0 }?.toString() ?: "Restricted")
                .build()
        )
        val vin = VehicleRepository.getVin()
        infoListBuilder.addItem(
            Row.Builder()
                .setTitle("VIN")
                .addText(vin.ifEmpty { "Restricted" })
                .build()
        )
        val fuelCap = VehicleRepository.getFuelCapacityLitres()
        infoListBuilder.addItem(
            Row.Builder()
                .setTitle("Fuel Capacity")
                .addText(
                    if (fuelCap > 0) {
                        String.format(Locale.US, "%.1f L", fuelCap)
                    } else {
                        "Restricted"
                    }
                )
                .build()
        )

        // ── System Health section ─────────────────────────────────────────
        val healthListBuilder = ItemList.Builder()

        val engineStatus = when {
            rpm > 5000 -> "Critical  —  High RPM (${rpm.toInt()})"
            rpm > 3500 -> "Warning  —  Elevated RPM (${rpm.toInt()})"
            rpm > 0    -> "Normal  —  ${rpm.toInt()} RPM"
            else       -> "Off"
        }
        healthListBuilder.addItem(
            Row.Builder()
                .setTitle("Engine")
                .addText(engineStatus)
                .build()
        )

        val fuelStatus = when {
            fuel < 10  -> "Critical  —  Refuel Now (${fuel.toInt()}%)"
            fuel < 25  -> "Low  —  Refuel Soon (${fuel.toInt()}%)"
            else       -> "Good  —  ${fuel.toInt()}% remaining"
        }
        healthListBuilder.addItem(
            Row.Builder()
                .setTitle("Fuel System")
                .addText(fuelStatus)
                .build()
        )

        val speedStatus = when {
            speed > 100 -> "Overspeed Alert  —  ${speed.toInt()} km/h"
            speed > 60  -> "High Speed  —  ${speed.toInt()} km/h"
            speed > 0   -> "Safe  —  ${speed.toInt()} km/h"
            else        -> "Parked"
        }
        healthListBuilder.addItem(
            Row.Builder()
                .setTitle("Driving Safety")
                .addText(speedStatus)
                .build()
        )

        // ── Issues section ────────────────────────────────────────────────
        val issuesListBuilder = ItemList.Builder()
        val alert = viewModel.currentAlert.value

        if (alert == null) {
            issuesListBuilder.addItem(
                Row.Builder()
                    .setTitle("No Issues Detected")
                    .addText("Vehicle is operating normally")
                    .build()
            )
        } else {
            issuesListBuilder.addItem(
                Row.Builder()
                    .setTitle(alert.message)
                    .addText("Severity: ${alert.severity.name}")
                    .build()
            )
            alert.dtcCode?.let { code ->
                issuesListBuilder.addItem(
                    Row.Builder()
                        .setTitle("DTC Code")
                        .addText(code)
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle("Vehicle Diagnostics")
            .setHeaderAction(Action.BACK)
            .addSectionedList(
                SectionedItemList.create(infoListBuilder.build(), "Vehicle Info  ·  Live VHAL")
            )
            .addSectionedList(
                SectionedItemList.create(healthListBuilder.build(), "System Health")
            )
            .addSectionedList(
                SectionedItemList.create(issuesListBuilder.build(), "Issues")
            )
            .build()
    }
}
