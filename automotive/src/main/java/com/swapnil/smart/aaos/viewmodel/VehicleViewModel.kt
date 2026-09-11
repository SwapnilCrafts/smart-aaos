package com.swapnil.smart.aaos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.swapnil.smart.aaos.utils.AlertRepository
import com.swapnil.smart.aaos.utils.VehicleAlert
import com.swapnil.smart.aaos.vehicle.VehicleRepository

/**
 * Vehicle state for the car screens.
 *
 * Entirely event-driven. Previously this polled the AIDL service every second
 * from a `while (true)` coroutine, which meant six Binder round trips per
 * second regardless of whether anything had changed - and, worse, could never
 * observe a value injected with `cmd car_service inject-vhal-event`, because
 * injection feeds the property event stream rather than the value
 * getProperty() returns.
 *
 * Now [VehicleRepository] pushes a snapshot whenever the VHAL reports a change
 * or a simulated value moves, and this simply mirrors it into LiveData. An
 * idle, parked vehicle produces no traffic at all.
 */
class VehicleViewModel : ViewModel() {

    private val _speed = MutableLiveData(0f)
    val speed: LiveData<Float> = _speed

    private val _rpm = MutableLiveData(0f)
    val rpm: LiveData<Float> = _rpm

    private val _fuel = MutableLiveData(0f)
    val fuel: LiveData<Float> = _fuel

    private val _gear = MutableLiveData("P")
    val gear: LiveData<String> = _gear

    private val _engineOn = MutableLiveData(false)
    val engineOn: LiveData<Boolean> = _engineOn

    private val _odometer = MutableLiveData(0f)
    val odometer: LiveData<Float> = _odometer

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _currentAlert = MutableLiveData<VehicleAlert?>(null)
    val currentAlert: LiveData<VehicleAlert?> = _currentAlert

    private val _isCarMoving = MutableLiveData(false)
    val isCarMoving: LiveData<Boolean> = _isCarMoving

    private val onVehicleData: () -> Unit = { publish() }
    private val onConnectionChanged: () -> Unit = {
        _isConnected.value = VehicleRepository.isConnected
        publish()
    }
    private val onAlertChanged: () -> Unit = {
        _currentAlert.value = AlertRepository.currentAlert
    }

    init {
        VehicleRepository.observe(onVehicleData)
        VehicleRepository.observeConnection(onConnectionChanged)
        AlertRepository.observe(onAlertChanged)

        // Seed from whatever is already known, in case the service connected
        // before this ViewModel existed.
        _isConnected.value = VehicleRepository.isConnected
        publish()
    }

    /** Mirrors the repository's latest snapshot into LiveData. */
    private fun publish() {
        val s = VehicleRepository.snapshot
        _speed.value = s.speedKmh
        _rpm.value = s.rpm
        _fuel.value = s.fuelPercent
        _gear.value = s.gear
        _engineOn.value = s.engineOn
        _odometer.value = s.odometerKm
        _isCarMoving.value = s.speedKmh > MOVING_THRESHOLD_KMH
        _currentAlert.value = AlertRepository.currentAlert
    }

    fun simulateDriving() = VehicleRepository.simulateDriving()

    fun simulateParked() = VehicleRepository.simulateParked()

    override fun onCleared() {
        super.onCleared()
        VehicleRepository.removeObserver(onVehicleData)
        VehicleRepository.removeConnectionObserver(onConnectionChanged)
        AlertRepository.removeObserver(onAlertChanged)
    }

    private companion object {
        const val MOVING_THRESHOLD_KMH = 2f
    }
}
