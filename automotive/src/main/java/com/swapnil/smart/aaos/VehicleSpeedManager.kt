package com.swapnil.smart.aaos

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log

object VehicleSpeedManager {

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private var speedListener: ((Float) -> Unit)? = null
    private var currentSpeedKmh: Float = 0f

    // ✅ VEHICLE_SPEED property ID — hardcoded value
    // Same as VehiclePropertyIds.VEHICLE_SPEED = 0x11600207
    private const val VEHICLE_SPEED_PROPERTY_ID = 0x11600207

    val isMoving: Boolean
        get() = currentSpeedKmh > 2f

    fun init(context: Context, onSpeedChanged: (Float) -> Unit) {
        speedListener = onSpeedChanged

        try {
            car = Car.createCar(context)
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE)
                    as? CarPropertyManager

            // ✅ Use hardcoded property ID
            carPropertyManager?.registerCallback(
                speedCallback,
                VEHICLE_SPEED_PROPERTY_ID,
                CarPropertyManager.SENSOR_RATE_ONCHANGE
            )

            Log.d("SmartAAOS", "VehicleSpeedManager initialized!")

        } catch (e: Exception) {
            Log.d("SmartAAOS", "Car API not available: ${e.message}")
            currentSpeedKmh = 0f
        }
    }

    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val speedMs = value.value as? Float ?: 0f
            currentSpeedKmh = speedMs * 3.6f
            Log.d("SmartAAOS", "Speed: $currentSpeedKmh km/h")
            speedListener?.invoke(currentSpeedKmh)
        }

        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.d("SmartAAOS", "Speed property error")
        }
    }

    fun release() {
        try {
            carPropertyManager?.unregisterCallback(speedCallback)
            car?.disconnect()
        } catch (e: Exception) {
            Log.d("SmartAAOS", "Release error: ${e.message}")
        }
    }

    fun simulateSpeed(kmh: Float) {
        currentSpeedKmh = kmh
        speedListener?.invoke(kmh)
        Log.d("SmartAAOS", "Simulated speed: $kmh km/h")
    }
}