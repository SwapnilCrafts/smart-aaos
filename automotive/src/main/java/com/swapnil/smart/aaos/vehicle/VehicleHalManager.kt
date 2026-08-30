package com.swapnil.smart.aaos.vehicle

import android.car.Car
import android.car.VehicleAreaType
import android.car.VehicleGear
import android.car.VehicleIgnitionState
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Reads real vehicle data from the VHAL. On the automotive emulator the
 * following properties are exposed and can be manipulated via the emulator's
 * "Car" extended-controls panel or `adb shell cmd car_service`, so this
 * returns live values when available. Falls back to the legacy simulated
 * values (from [VehicleDataService]) when the car service / property is not
 * supported on the current device.
 *
 * Nullable so a failed read never crashes the app.
 */
class VehicleHalManager(context: Context) {

    companion object {
        private const val TAG = "SmartAAOS_VHAL"
        private const val AREA_GLOBAL = VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
    }

    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null

    private fun ensureConnected() {
        if (propertyManager != null) return
        val c = car ?: return
        // Some emulator builds deliver the connection callback late (or not at
        // all) even though the car service is up. Tolerate that by attempting
        // to acquire the property manager directly; on a user build this throws
        // CarNotConnectedException which we swallow and fall back to simulation.
        try {
            propertyManager =
                c.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            if (propertyManager != null) Log.d(TAG, "CarPropertyManager acquired")
        } catch (e: Exception) {
            Log.d(TAG, "Car service unavailable (${e::class.java.simpleName}): ${e.message}")
        }
    }

    private val lifecycleListener = Car.CarServiceLifecycleListener { car, connected ->
        Log.d(TAG, "Car service lifecycle changed: connected=$connected")
        if (connected) ensureConnected() else propertyManager = null
    }

    init {
        try {
            car = Car.createCar(
                context,
                Handler(Looper.getMainLooper()),
                Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER,
                lifecycleListener
            )
            Log.d(TAG, "Car created: $car")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VHAL access: ${e.message}")
        }
    }

    /** True if the real VHAL is reachable and exposes properties. */
    val isAvailable: Boolean
        get() {
            ensureConnected()
            return propertyManager != null
        }

    private fun readFloat(propertyId: Int): Float? {
        ensureConnected()
        val pm = propertyManager ?: return null
        return try {
            val value = pm.getProperty(Float::class.java, propertyId, AREA_GLOBAL)
            if (value?.status != CarPropertyValue.STATUS_AVAILABLE) return null
            value.value
        } catch (e: Exception) {
            Log.d(TAG, "Read float $propertyId failed: ${e.message}")
            null
        }
    }

    private fun readInt(propertyId: Int): Int? {
        ensureConnected()
        val pm = propertyManager ?: return null
        return try {
            val value = pm.getProperty(Int::class.java, propertyId, AREA_GLOBAL)
            if (value?.status != CarPropertyValue.STATUS_AVAILABLE) return null
            value.value
        } catch (e: Exception) {
            Log.d(TAG, "Read int $propertyId failed: ${e.message}")
            null
        }
    }

    private fun readString(propertyId: Int): String? {
        ensureConnected()
        val pm = propertyManager ?: return null
        return try {
            val value = pm.getProperty(String::class.java, propertyId, AREA_GLOBAL)
            if (value?.status != CarPropertyValue.STATUS_AVAILABLE) return null
            value.value
        } catch (e: Exception) {
            Log.d(TAG, "Read string $propertyId failed: ${e.message}")
            null
        }
    }

    /** Kilometers per hour, or null when unavailable. */
    fun getSpeedKmh(): Float? {
        val khm = readInt(VehiclePropertyIds.PERF_VEHICLE_SPEED) ?: return null
        // VHAL exposes speed in hundredths of a km/h.
        return khm / 100f
    }

    /** Engine RPM, or null when unavailable. */
    fun getRpm(): Float? {
        return readInt(VehiclePropertyIds.ENGINE_RPM)?.toFloat()
    }

    /** Fuel level 0.0..1.0, or null when unavailable. */
    fun getFuelLevelFraction(): Float? {
        val fraction = readFloat(VehiclePropertyIds.FUEL_LEVEL) ?: return null
        return fraction.coerceIn(0f, 1f)
    }

    /** Current gear as a display string, or null when unavailable. */
    fun getGearString(): String? {
        val gearInt = readInt(VehiclePropertyIds.CURRENT_GEAR) ?: return null
        // VehicleGear.toString returns the human-readable gear name.
        return if (gearInt == VehicleGear.GEAR_UNKNOWN) null else VehicleGear.toString(gearInt)
    }

    /** True when the engine is running (ignition state ON/START), or null if unknown. */
    fun isEngineOn(): Boolean? {
        val ignition = readInt(VehiclePropertyIds.IGNITION_STATE) ?: return null
        return ignition == VehicleIgnitionState.ON || ignition == VehicleIgnitionState.START
    }

    /** Odometer in kilometers, or null when unavailable. */
    fun getOdometerKm(): Float? {
        val meters = readInt(VehiclePropertyIds.PERF_ODOMETER) ?: return null
        return meters / 1000f
    }

    /** Battery state of charge 0.0..1.0, or null when unavailable. */
    fun getBatteryLevelFraction(): Float? {
        val fraction = readFloat(VehiclePropertyIds.EV_BATTERY_LEVEL) ?: return null
        return fraction.coerceIn(0f, 1f)
    }

    /** True when the driver's seatbelt is buckled, or null when unavailable. */
    fun isDriverSeatbeltOn(): Boolean? {
        // SEAT_BELT_BUCKLED is a bitfield where bit0 = driver.
        val state = readInt(VehiclePropertyIds.SEAT_BELT_BUCKLED) ?: return null
        return (state and 0x1) == 0x1
    }

    // ── Vehicle INFO properties ──────────────────────────────────────────────
    // Unlike the speed/RPM/fuel/gear properties above, these INFO_* properties
    // are gated by the install-time (normal) permission android.car.permission
    // .CAR_INFO, which a third-party app IS granted. That means they read real,
    // live values from the VHAL (e.g. on the emulator: make/model/vin/capacity).
    // These are the ones we can actually demo as "real" VHAL data.

    /** Vehicle make, e.g. "Toy Vehicle", or null when unavailable. */
    fun getMake(): String? = readString(VehiclePropertyIds.INFO_MAKE)

    /** Vehicle model, e.g. "Speedy Model", or null when unavailable. */
    fun getModel(): String? = readString(VehiclePropertyIds.INFO_MODEL)

    /** Vehicle VIN, or null when unavailable. */
    fun getVin(): String? = readString(VehiclePropertyIds.INFO_VIN)

    /** Model year as an Int, or null when unavailable. */
    fun getModelYear(): Int? = readInt(VehiclePropertyIds.INFO_MODEL_YEAR)

    /** Fuel tank capacity in litres (VHAL reports millilitres), or null. */
    fun getFuelCapacityLitres(): Float? {
        val ml = readFloat(VehiclePropertyIds.INFO_FUEL_CAPACITY) ?: return null
        return ml / 1000f
    }

    /** EV battery capacity in kWh (VHAL reports watt-hours), or null. */
    fun getBatteryCapacityKwh(): Float? {
        val wh = readFloat(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY) ?: return null
        return wh / 1000f
    }

    fun release() {
        try {
            car?.disconnect()
            car = null
            propertyManager = null
        } catch (e: Exception) {
            Log.d(TAG, "Release failed: ${e.message}")
        }
    }
}