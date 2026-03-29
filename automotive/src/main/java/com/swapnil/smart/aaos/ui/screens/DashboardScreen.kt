package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.Observer
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel

class DashboardScreen(carContext: CarContext) : Screen(carContext) {

    // ✅ Get ViewModel from our custom store
    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    init {
        // ✅ Observe LiveData — invalidate screen when data changes
        viewModel.speed.observeForever { invalidate() }
        viewModel.rpm.observeForever { invalidate() }
        viewModel.fuel.observeForever { invalidate() }
        viewModel.gear.observeForever { invalidate() }
        viewModel.isConnected.observeForever { invalidate() }
        viewModel.currentAlert.observeForever { invalidate() }
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // ✅ Read from ViewModel instead of direct repository calls
        viewModel.currentAlert.value?.let { alert ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🚨 ${alert.message}")
                    .addText("Tap to view details")
                    .setOnClickListener {
                        screenManager.push(DiagnosticsScreen(carContext))
                    }
                    .build()
            )
        }

        if (viewModel.isConnected.value != true) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🔄 Connecting to Vehicle Service...")
                    .addText("Please wait")
                    .build()
            )
        } else {
            val speed   = viewModel.speed.value ?: 0f
            val rpm     = viewModel.rpm.value ?: 0f
            val fuel    = viewModel.fuel.value ?: 0f
            val gear    = viewModel.gear.value ?: "P"
            val engineOn = viewModel.engineOn.value ?: false
            val odometer = viewModel.odometer.value ?: 0f

            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🚗 Speed")
                    .addText("${speed.toInt()} km/h")
                    .build()
            )
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⚙️ RPM")
                    .addText("${rpm.toInt()} RPM")
                    .build()
            )
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⛽ Fuel")
                    .addText("${fuel.toInt()}%")
                    .build()
            )
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🔧 Gear")
                    .addText("$gear")
                    .addText(if (engineOn) "Engine ON" else "Engine OFF")
                    .build()
            )
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("📍 Odometer")
                    .addText("${String.format("%.1f", odometer)} km")
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle(
                if (viewModel.isConnected.value == true) "🚗 Dashboard — MVVM"
                else "🚗 Dashboard — Connecting..."
            )
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle(
                                if ((viewModel.speed.value ?: 0f) > 2f) "🅿️ Park"
                                else "🚗 Drive"
                            )
                            .setOnClickListener {
                                if ((viewModel.speed.value ?: 0f) > 2f) {
                                    viewModel.simulateParked()
                                } else {
                                    viewModel.simulateDriving()
                                }
                            }
                            .build()
                    )
                    .build()
            )
            .setSingleList(listBuilder.build())
            .build()
    }
}