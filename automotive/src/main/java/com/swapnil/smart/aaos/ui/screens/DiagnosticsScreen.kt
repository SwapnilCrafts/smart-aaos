package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.swapnil.smart.aaos.utils.AlertRepository
import com.swapnil.smart.aaos.vehicle.VehicleRepository
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel

class DiagnosticsScreen(carContext: CarContext) : Screen(carContext) {

    // ✅ shared ViewModel
    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    init {
        viewModel.currentAlert.observeForever { invalidate() }
        viewModel.speed.observeForever { invalidate() }
        viewModel.rpm.observeForever { invalidate() }
        viewModel.fuel.observeForever { invalidate() }
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // ✅ read from ViewModel
        val speed = viewModel.speed.value ?: 0f
        val rpm   = viewModel.rpm.value ?: 0f
        val fuel  = viewModel.fuel.value ?: 0f

        listBuilder.addItem(
            Row.Builder()
                .setTitle("🧠 Engine System")
                .addText(when {
                    rpm > 5000 -> "Critical 🚨"
                    rpm > 3500 -> "Warning ⚠️"
                    rpm > 0    -> "Normal ✅"
                    else       -> "Off"
                })
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle("⛽ Fuel System")
                .addText(when {
                    fuel < 10  -> "Critical 🚨"
                    fuel < 25  -> "Low ⚠️"
                    else       -> "Good ✅"
                })
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle("🚗 Driving Safety")
                .addText(when {
                    speed > 100 -> "Overspeed 🚨"
                    speed > 60  -> "High Speed ⚠️"
                    speed > 0   -> "Safe ✅"
                    else        -> "Parked"
                })
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle("──────── Issues ────────")
                .build()
        )

        val alert = viewModel.currentAlert.value

        if (alert == null) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("✅ No Issues Detected")
                    .addText("Vehicle is healthy")
                    .build()
            )
        } else {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⚠️ Issue Detected")
                    .addText(alert.message)
                    .build()
            )
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Severity")
                    .addText(alert.severity.name)
                    .build()
            )
            alert.dtcCode?.let { code ->
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle("🔧 $code")
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle("📊 Vehicle Diagnostics")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }
}