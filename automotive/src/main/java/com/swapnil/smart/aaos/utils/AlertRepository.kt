package com.swapnil.smart.aaos.utils


import com.swapnil.smart.aaos.utils.Severity
import com.swapnil.smart.aaos.utils.VehicleAlert
import com.swapnil.smart.aaos.vehicle.VehicleRepository

object AlertRepository {

    var currentAlert: VehicleAlert? = null
        private set

    private var manualAlert: VehicleAlert? = null

    private val listeners = mutableListOf<() -> Unit>()

    private var isRunning = false

    /** Re-evaluate whenever the vehicle pushes new state. */
    private val onVehicleData: () -> Unit = { evaluateAlerts() }

    /**
     * Starts evaluating alerts. Driven by [VehicleRepository]'s push updates
     * rather than a 1 Hz Handler loop - thresholds can only be crossed when a
     * value actually changes, so a timer was pure overhead. Idempotent.
     */
    fun start() {
        if (isRunning) return
        isRunning = true
        VehicleRepository.observe(onVehicleData)
        evaluateAlerts()
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        VehicleRepository.removeObserver(onVehicleData)
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
        val snapshot = VehicleRepository.snapshot
        val speed = snapshot.speedKmh
        val rpm = snapshot.rpm
        val fuel = snapshot.fuelPercent

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
        // Re-derive from real values now; nothing else will until the vehicle
        // pushes its next update.
        evaluateAlerts()
    }

    fun observe(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeObserver(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }
}
