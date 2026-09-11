package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import com.swapnil.smart.aaos.utils.AlertRepository
import com.swapnil.smart.aaos.utils.Severity
import com.swapnil.smart.aaos.utils.VehicleAlert
import com.swapnil.smart.aaos.vehicle.VehicleRepository
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel
import java.util.Locale

/**
 * Debug harness for the alert and diagnostics paths.
 *
 * [AlertRepository] only raises an alert above speed 100, rpm 5000 or below
 * fuel 10, but the only simulator wired to the UI is `simulateDriving()` at
 * 60 km/h / 2000 rpm / 70% fuel - which crosses no threshold. That left the
 * alert rows, the Diagnostics "Issues" section, the DTC row and the gauge
 * alert colouring unreachable. These rows drive the scenario helpers that
 * already existed in [VehicleRepository] but had no call sites.
 */
class SimulationScreen(carContext: CarContext) : Screen(carContext) {

    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    private var lastAlert: VehicleAlert? = null
    private var lastSpeed = Float.MIN_VALUE

    init {
        // Guarded so the 1 Hz poll doesn't rebuild the template every tick.
        viewModel.currentAlert.observe(this) {
            if (it != lastAlert) {
                lastAlert = it
                invalidate()
            }
        }
        viewModel.speed.observe(this) {
            if (it != lastSpeed) {
                lastSpeed = it
                invalidate()
            }
        }
    }

    private fun apply(label: String, action: () -> Unit) {
        action()
        CarToast.makeText(carContext, label, CarToast.LENGTH_SHORT).show()
        invalidate()
    }

    private fun scenarioRow(title: String, subtitle: String, action: () -> Unit) =
        Row.Builder()
            .setTitle(title)
            .addText(subtitle)
            .setOnClickListener { apply(title, action) }
            .build()

    override fun onGetTemplate(): Template {
        val scenarios = ItemList.Builder()
            .addItem(
                scenarioRow("Normal driving", "50 km/h · 2000 rpm · 60% fuel · no alert") {
                    AlertRepository.clearManualAlert()
                    VehicleRepository.simulateNormalDriving()
                }
            )
            .addItem(
                scenarioRow("Overspeed", "120 km/h · expects HIGH alert") {
                    AlertRepository.clearManualAlert()
                    VehicleRepository.simulateOverspeed()
                }
            )
            .addItem(
                scenarioRow("Engine fault", "5500 rpm · expects MEDIUM alert") {
                    AlertRepository.clearManualAlert()
                    VehicleRepository.simulateEngineFault()
                }
            )
            .addItem(
                scenarioRow("Low fuel", "8% fuel · expects LOW alert") {
                    AlertRepository.clearManualAlert()
                    VehicleRepository.simulateLowFuel()
                }
            )
            .addItem(
                scenarioRow("All critical", "140 km/h · 6000 rpm · 5% fuel") {
                    AlertRepository.clearManualAlert()
                    VehicleRepository.simulateCriticalAll()
                }
            )
            .addItem(
                scenarioRow("Park", "Speed 0 · gear P · clears alerts") {
                    AlertRepository.clearManualAlert()
                    VehicleRepository.simulateParked()
                }
            )
            .build()

        // Manual alerts override the threshold logic, and are the only way to
        // exercise the DTC code row in DiagnosticsScreen.
        val manual = ItemList.Builder()
            .addItem(
                scenarioRow("Trigger DTC alert", "Misfire · HIGH · code P0301") {
                    AlertRepository.triggerManualAlert(
                        VehicleAlert("Cylinder 1 Misfire", Severity.HIGH, "P0301")
                    )
                }
            )
            .addItem(
                scenarioRow("Clear manual alert", "Return to threshold-based alerts") {
                    AlertRepository.clearManualAlert()
                }
            )
            .build()

        val speed = viewModel.speed.value ?: 0f
        val rpm = viewModel.rpm.value ?: 0f
        val fuel = viewModel.fuel.value ?: 0f
        val alert = viewModel.currentAlert.value

        val state = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle(
                        String.format(
                            Locale.US, "%.0f km/h · %.0f rpm · %.0f%% fuel",
                            speed, rpm, fuel
                        )
                    )
                    .addText(
                        alert?.let { "Alert: ${it.message} (${it.severity.name})" }
                            ?: "No alert active"
                    )
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("Simulation (debug)")
            .setHeaderAction(Action.BACK)
            .addSectionedList(SectionedItemList.create(state, "Current State"))
            .addSectionedList(SectionedItemList.create(scenarios, "Scenarios"))
            .addSectionedList(SectionedItemList.create(manual, "Manual Alert"))
            .build()
    }
}
