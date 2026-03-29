package com.swapnil.smart.aaos.utils


import android.os.Handler
import android.os.Looper
import com.swapnil.smart.aaos.utils.Severity
import com.swapnil.smart.aaos.utils.VehicleAlert
import com.swapnil.smart.aaos.vehicle.VehicleRepository

object AlertRepository {

    private val handler = Handler(Looper.getMainLooper())

    var currentAlert: VehicleAlert? = null
        private set

    private var manualAlert: VehicleAlert? = null // 🔥 NEW

    private val listeners = mutableListOf<() -> Unit>()

    private val runnable = object : Runnable {
        override fun run() {
            evaluateAlerts()
            handler.postDelayed(this, 1000)
        }
    }

    fun start() {
        handler.post(runnable)
    }

    private fun evaluateAlerts() {

        // 🔥 PRIORITY 1: MANUAL ALERT (for testing)
        if (manualAlert != null) {
            if (currentAlert != manualAlert) {
                currentAlert = manualAlert
                notifyListeners()
            }
            return
        }

        // 🔥 PRIORITY 2: AUTO ALERT
        val speed = VehicleRepository.getSpeed()
        val rpm = VehicleRepository.getRpm()
        val fuel = VehicleRepository.getFuel()

        val newAlert = when {
            speed > 100 -> VehicleAlert("Overspeed!", Severity.HIGH)
            rpm > 5000 -> VehicleAlert("Engine Overstress", Severity.MEDIUM)
            fuel < 10 -> VehicleAlert("Low Fuel", Severity.LOW)
            else -> null
        }

        if (newAlert != currentAlert) {
            currentAlert = newAlert
            notifyListeners()
        }
    }

    // 🔥 MANUAL CONTROL METHODS

    fun triggerManualAlert(alert: VehicleAlert) {
        manualAlert = alert
        currentAlert = alert
        notifyListeners()
    }

    fun clearManualAlert() {
        manualAlert = null
    }

    fun observe(listener: () -> Unit) {
        listeners.add(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }
}
