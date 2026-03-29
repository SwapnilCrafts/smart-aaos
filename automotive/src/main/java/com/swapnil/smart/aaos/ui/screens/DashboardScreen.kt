package com.swapnil.smart.aaos.ui.screens
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.swapnil.smart.aaos.utils.AlertRepository
import com.swapnil.smart.aaos.utils.Severity
import com.swapnil.smart.aaos.utils.VehicleAlert
import com.swapnil.smart.aaos.vehicle.VehicleRepository

class DashboardScreen(carContext: CarContext) : Screen(carContext) {

    companion object {
        private const val TAG = "SmartAAOS_AIDL"
    }



    private var isSimulating = false


    // AIDL Service Connection

    private val handler = Handler(Looper.getMainLooper())

    init {
        handler.post(object : Runnable {
            override fun run() {
                invalidate()
                handler.postDelayed(this, 1000L)
            }
        })
    }
    override fun onGetTemplate(): Template {

        val listBuilder = ItemList.Builder()

        AlertRepository.currentAlert?.let { alert ->
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
        if (!VehicleRepository.isConnected) {
            // ✅ ONLY show simple rows (no loading state)
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🔄 Connecting to Vehicle Service...")
                    .addText("Please wait")
                    .build()
            )

        } else {
            // ✅ Safe AIDL calls
            val speed = try { VehicleRepository.getSpeed() ?: 0f } catch (e: Exception) { 0f }
            val rpm = try { VehicleRepository.getRpm() ?: 0f } catch (e: Exception) { 0f }
            val fuel = try { VehicleRepository.getFuel() ?: 0f } catch (e: Exception) { 0f }
            val gear = try { VehicleRepository.getGear() ?: "P" } catch (e: Exception) { "P" }
            val engineOn = try { VehicleRepository.isEngineOn() ?: false } catch (e: Exception) { false }
            val odometer = try { VehicleRepository.getOdometer() ?: 0f } catch (e: Exception) { 0f }


            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🧪 Test Overspeed Alert")
                    .setOnClickListener {
                        AlertRepository.triggerManualAlert(
                            VehicleAlert("Overspeed!", Severity.HIGH)
                        )
                    }
                    .build()
            )

            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🧪 Clear Alert")
                    .setOnClickListener {
                        AlertRepository.clearManualAlert()
                    }
                    .build()
            )
            // Speed
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🚗 Speed")
                    .addText("${speed.toInt()} km/h")
                    .build()
            )

            // RPM
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⚙️ RPM")
                    .addText("${rpm.toInt()} RPM")
                    .build()
            )

            // Fuel
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⛽ Fuel")
                    .addText("${fuel.toInt()}%")
                    .build()
            )

            // Gear
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🔧 Gear")
                    .addText("$gear")
                    .addText(if (engineOn) "Engine ON" else "Engine OFF")
                    .build()
            )

            // Odometer
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("📍 Odometer")
                    .addText("${String.format("%.1f", odometer)} km")
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle(
                if (VehicleRepository.isConnected) "🚗 Dashboard — AIDL"
                else "🚗 Dashboard — Connecting..."
            )
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle(
                                if (VehicleRepository.getSpeed() > 2f) "🅿️ Park"
                                else "🚗 Drive"
                            )
                            .setOnClickListener {
                                val speed = VehicleRepository.getSpeed()
                                if (speed > 2f) {
                                    VehicleRepository.simulateParked()
                                } else {
                                    VehicleRepository.simulateDriving()
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

}