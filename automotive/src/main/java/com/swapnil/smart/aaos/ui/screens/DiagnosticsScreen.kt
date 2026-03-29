package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.swapnil.smart.aaos.utils.AlertRepository
import com.swapnil.smart.aaos.vehicle.VehicleRepository

class DiagnosticsScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {

        val listBuilder = ItemList.Builder()

        // 🔹 Get vehicle data (ONLY for SMART VIEW)
        val speed = VehicleRepository.getSpeed()
        val rpm = VehicleRepository.getRpm()
        val fuel = VehicleRepository.getFuel()

        // 🔹 SMART VIEW (keep this — it's good UX)
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🧠 Engine System")
                .addText(
                    when {
                        rpm > 5000 -> "Critical 🚨"
                        rpm > 3500 -> "Warning ⚠️"
                        rpm > 0 -> "Normal ✅"
                        else -> "Off"
                    }
                )
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle("⛽ Fuel System")
                .addText(
                    when {
                        fuel < 10 -> "Critical 🚨"
                        fuel < 25 -> "Low ⚠️"
                        else -> "Good ✅"
                    }
                )
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle("🚗 Driving Safety")
                .addText(
                    when {
                        speed > 100 -> "Overspeed 🚨"
                        speed > 60 -> "High Speed ⚠️"
                        speed > 0 -> "Safe ✅"
                        else -> "Parked"
                    }
                )
                .build()
        )

        // 🔹 Divider
        listBuilder.addItem(
            Row.Builder()
                .setTitle("──────── Issues ────────")
                .build()
        )

        // 🔥 SINGLE SOURCE OF TRUTH (AlertRepository)
        val alert = AlertRepository.currentAlert

        if (alert == null) {

            listBuilder.addItem(
                Row.Builder()
                    .setTitle("✅ No Issues Detected")
                    .addText("Vehicle is healthy")
                    .build()
            )

        } else {

            // 🔹 Issue
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⚠️ Issue Detected")
                    .addText(alert.message)
                    .build()
            )

            // 🔹 Severity
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Severity")
                    .addText(alert.severity.name)
                    .build()
            )

            // 🔹 DTC Code
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