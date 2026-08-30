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

class DiagnosticsScreen(carContext: CarContext) : Screen(carContext) {

    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    init {
        viewModel.currentAlert.observeForever { invalidate() }
        viewModel.speed.observeForever { invalidate() }
        viewModel.rpm.observeForever { invalidate() }
        viewModel.fuel.observeForever { invalidate() }
    }

    override fun onGetTemplate(): Template {
        val speed = viewModel.speed.value ?: 0f
        val rpm   = viewModel.rpm.value ?: 0f
        val fuel  = viewModel.fuel.value ?: 0f

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
                SectionedItemList.create(healthListBuilder.build(), "System Health")
            )
            .addSectionedList(
                SectionedItemList.create(issuesListBuilder.build(), "Issues")
            )
            .build()
    }
}
