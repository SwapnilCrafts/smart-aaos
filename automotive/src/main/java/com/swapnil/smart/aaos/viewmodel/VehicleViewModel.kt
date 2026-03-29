package com.swapnil.smart.aaos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swapnil.smart.aaos.utils.AlertRepository
import com.swapnil.smart.aaos.utils.VehicleAlert
import com.swapnil.smart.aaos.vehicle.VehicleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VehicleViewModel : ViewModel() {

    // ✅ LiveData for each vehicle property
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

    // ✅ Poll vehicle data every second using coroutine
    init {
        viewModelScope.launch {
            while (true) {
                refreshVehicleData()
                delay(1000L)
            }
        }
    }

    private fun refreshVehicleData() {
        _isConnected.postValue(VehicleRepository.isConnected)

        if (VehicleRepository.isConnected) {
            val speed = VehicleRepository.getSpeed()
            _speed.postValue(speed)
            _rpm.postValue(VehicleRepository.getRpm())
            _fuel.postValue(VehicleRepository.getFuel())
            _gear.postValue(VehicleRepository.getGear())
            _engineOn.postValue(VehicleRepository.isEngineOn())
            _odometer.postValue(VehicleRepository.getOdometer())
            _isCarMoving.postValue(speed > 2f)
        }

        _currentAlert.postValue(AlertRepository.currentAlert)
    }

    fun simulateDriving() {
        VehicleRepository.simulateDriving()
        refreshVehicleData()
    }

    fun simulateParked() {
        VehicleRepository.simulateParked()
        refreshVehicleData()
    }
}