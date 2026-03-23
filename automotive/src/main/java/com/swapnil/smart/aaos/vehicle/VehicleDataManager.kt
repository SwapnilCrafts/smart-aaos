package com.swapnil.smart.aaos.vehicle

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log

data class VehicleData(
    val speedKmh: Float = 0f,
    val rpm: Float = 0f,
    val fuelPercent: Float = 100f,
    val gear: String = "P",
    val isEngineOn: Boolean = false,
    val odometerKm: Float = 0f
)

object VehicleDataManager {

    // ✅ VHAL Property IDs
    private const val PROP_SPEED        = 0x11600207  // PERF_VEHICLE_SPEED
    private const val PROP_RPM          = 0x11600305  // ENGINE_RPM
    private const val PROP_FUEL         = 0x11600307  // FUEL_LEVEL
    private const val PROP_GEAR         = 0x11400400  // GEAR_SELECTION
    private const val PROP_ENGINE_ON    = 0x11200401  // ENGINE_ON
    private const val PROP_ODOMETER     = 0x11600101  // PERF_ODOMETER

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private var dataListener: ((VehicleData) -> Unit)? = null

    var currentData = VehicleData()
        private set

    fun init(context: Context, onDataChanged: (VehicleData) -> Unit) {
        dataListener = onDataChanged

        try {
            car = Car.createCar(context)
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE)
                    as? CarPropertyManager

            // ✅ Register for all properties
            registerProperty(PROP_SPEED)
            registerProperty(PROP_RPM)
            registerProperty(PROP_FUEL)
            registerProperty(PROP_GEAR)
            registerProperty(PROP_ENGINE_ON)
            registerProperty(PROP_ODOMETER)

            Log.d("SmartAAOS", "VehicleDataManager initialized!")

        } catch (e: Exception) {
            Log.d("SmartAAOS", "Car API not available: ${e.message}")
            // Use simulated data on emulator
            simulateData()
        }
    }

    private fun registerProperty(propId: Int) {
        try {
            carPropertyManager?.registerCallback(
                propertyCallback,
                propId,
                CarPropertyManager.SENSOR_RATE_ONCHANGE
            )
        } catch (e: Exception) {
            Log.d("SmartAAOS", "Property $propId not available: ${e.message}")
        }
    }

    private val propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                PROP_SPEED -> {
                    val speedMs = value.value as? Float ?: 0f
                    currentData = currentData.copy(speedKmh = speedMs * 3.6f)
                }
                PROP_RPM -> {
                    currentData = currentData.copy(
                        rpm = value.value as? Float ?: 0f
                    )
                }
                PROP_FUEL -> {
                    currentData = currentData.copy(
                        fuelPercent = value.value as? Float ?: 0f
                    )
                }
                PROP_GEAR -> {
                    val gearInt = value.value as? Int ?: 0
                    currentData = currentData.copy(gear = gearToString(gearInt))
                }
                PROP_ENGINE_ON -> {
                    currentData = currentData.copy(
                        isEngineOn = value.value as? Boolean ?: false
                    )
                }
                PROP_ODOMETER -> {
                    currentData = currentData.copy(
                        odometerKm = value.value as? Float ?: 0f
                    )
                }
            }
            dataListener?.invoke(currentData)
            Log.d("SmartAAOS", "Vehicle data updated: $currentData")
        }

        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.d("SmartAAOS", "Property error: $propId")
        }
    }

    // ✅ Convert gear int to string
    private fun gearToString(gear: Int): String {
        return when (gear) {
            1 -> "P"   // Park
            2 -> "R"   // Reverse
            3 -> "N"   // Neutral
            4 -> "D"   // Drive
            else -> "D"
        }
    }

    // ✅ Simulate vehicle data for emulator
    fun simulateData(
        speedKmh: Float = 0f,
        rpm: Float = 800f,
        fuelPercent: Float = 75f,
        gear: String = "P",
        isEngineOn: Boolean = true,
        odometerKm: Float = 12450f
    ) {
        currentData = VehicleData(
            speedKmh = speedKmh,
            rpm = rpm,
            fuelPercent = fuelPercent,
            gear = gear,
            isEngineOn = isEngineOn,
            odometerKm = odometerKm
        )
        dataListener?.invoke(currentData)
        Log.d("SmartAAOS", "Simulated data: $currentData")
    }

    fun release() {
        try {
            carPropertyManager?.unregisterCallback(propertyCallback)
            car?.disconnect()
        } catch (e: Exception) {
            Log.d("SmartAAOS", "Release error: ${e.message}")
        }
    }
}