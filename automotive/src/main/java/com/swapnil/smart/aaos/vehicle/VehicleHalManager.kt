package com.swapnil.smart.aaos.vehicle

import android.car.Car
import android.car.VehicleAreaSeat
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
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads real vehicle data from the VHAL.
 *
 * Every getter is nullable: null means "not readable here", and
 * [VehicleDataService] then serves its simulated value instead. So a missing
 * permission or an unsupported property degrades rather than crashing.
 *
 * What a normal third-party install can actually read (verified against the
 * AAOS API 35 emulator with `adb shell cmd car_service get-carpropertyconfig`):
 *
 *   readable — CAR_INFO + CAR_POWERTRAIN are protectionLevel:normal
 *     INFO_MAKE / INFO_MODEL / INFO_MODEL_YEAR / INFO_FUEL_CAPACITY /
 *     INFO_EV_BATTERY_CAPACITY, GEAR_SELECTION / CURRENT_GEAR, IGNITION_STATE
 *
 *   NOT readable — signature|privileged, cannot be adb-granted
 *     PERF_VEHICLE_SPEED (CAR_SPEED), ENGINE_RPM (CAR_ENGINE_DETAILED),
 *     FUEL_LEVEL / EV_BATTERY_LEVEL (CAR_ENERGY), PERF_ODOMETER (CAR_MILEAGE),
 *     INFO_VIN (CAR_IDENTIFICATION)
 *
 * The blocked set only becomes readable if the app is installed as a
 * privileged system app; see the notes in README / VehicleDataService.
 */
class VehicleHalManager(context: Context) {

    companion object {
        private const val TAG = "SmartAAOS_VHAL"
        private const val AREA_GLOBAL = VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL

        /** Driver seat on a left-hand-drive vehicle. */
        private const val AREA_DRIVER_SEAT = VehicleAreaSeat.SEAT_ROW_1_LEFT

        /**
         * Properties to subscribe to, with the requested rate.
         *
         * CONTINUOUS properties take a sample rate (speed and RPM are capped
         * at 10 Hz by their config, fuel and battery at 100 Hz);
         * SENSOR_RATE_ONCHANGE is the correct rate for ON_CHANGE properties
         * such as gear and ignition. Entries the app lacks permission for are
         * skipped by isSupported(), so this table can list everything the app
         * would like without failing.
         */
        private val SUBSCRIPTIONS: List<Pair<Int, Float>> = listOf(
            VehiclePropertyIds.PERF_VEHICLE_SPEED to CarPropertyManager.SENSOR_RATE_UI,
            VehiclePropertyIds.ENGINE_RPM to CarPropertyManager.SENSOR_RATE_UI,
            VehiclePropertyIds.FUEL_LEVEL to CarPropertyManager.SENSOR_RATE_NORMAL,
            VehiclePropertyIds.EV_BATTERY_LEVEL to CarPropertyManager.SENSOR_RATE_NORMAL,
            VehiclePropertyIds.PERF_ODOMETER to CarPropertyManager.SENSOR_RATE_NORMAL,
            VehiclePropertyIds.GEAR_SELECTION to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.CURRENT_GEAR to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.IGNITION_STATE to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.PARKING_BRAKE_ON to CarPropertyManager.SENSOR_RATE_ONCHANGE
        )
    }

    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null

    /**
     * Latest value pushed by the VHAL, keyed by property and area.
     *
     * Reads prefer this over a fresh getProperty() call: it is the subscribed
     * value, and it is the only way to observe anything injected with
     * `cmd car_service inject-vhal-event` - injection feeds the property event
     * stream, not the stored value getProperty() returns.
     */
    private val latest = ConcurrentHashMap<Long, Any>()

    private var eventCallback: CarPropertyManager.CarPropertyEventCallback? = null
    private var onEvent: (() -> Unit)? = null

    /**
     * Per-property read permission / support verdict.
     *
     * Without this, [isSupported] asks the car service on every poll, and each
     * miss makes the framework log "W CarPropertyManager: Missing required
     * permissions to access property: X" - at 1 Hz across four blocked
     * properties that is 4 warnings a second drowning logcat. The verdict can
     * only change when the car service reconnects, so cache it and clear the
     * cache on disconnect.
     */
    private val supportCache = ConcurrentHashMap<Int, Boolean>()

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

    private val lifecycleListener = Car.CarServiceLifecycleListener { _, connected ->
        Log.d(TAG, "Car service lifecycle changed: connected=$connected")
        if (connected) {
            ensureConnected()
        } else {
            propertyManager = null
            supportCache.clear()
        }
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

    // ── Raw reads ────────────────────────────────────────────────────────────
    // Deliberately uses the non-generic getProperty(propId, areaId) overload and
    // casts the payload ourselves. The generic getProperty(Class<E>, ...) overload
    // type-checks against the class we pass, and Kotlin's `Float::class.java`
    // resolves to PRIMITIVE float.class, which never matches the boxed
    // java.lang.Float the HAL returns — so that overload rejects even correctly
    // typed properties. Casting the value avoids the trap entirely.

    /**
     * True when the property is supported AND this app holds its permission.
     * Answered from [supportCache] after the first lookup per connection.
     */
    private fun isSupported(propertyId: Int): Boolean {
        ensureConnected()
        val pm = propertyManager ?: return false

        supportCache[propertyId]?.let { return it }

        val supported = try {
            // Returns null when unsupported or when we lack the read permission,
            // so this is a permission check that costs no exception.
            pm.getCarPropertyConfig(propertyId) != null
        } catch (e: Exception) {
            Log.d(TAG, "Config lookup $propertyId failed: ${e.message}")
            false
        }
        supportCache[propertyId] = supported
        return supported
    }

    private fun cacheKey(propertyId: Int, areaId: Int): Long =
        (propertyId.toLong() shl 32) or (areaId.toLong() and 0xFFFFFFFFL)

    private fun readValue(propertyId: Int, areaId: Int = AREA_GLOBAL): Any? {
        // Subscribed value first; fall back to a direct read until the first
        // event arrives (or when the subscription could not be established).
        latest[cacheKey(propertyId, areaId)]?.let { return it }
        if (!isSupported(propertyId)) return null
        val pm = propertyManager ?: return null
        return try {
            // getProperty(int, int) is <E> CarPropertyValue<E>, so E must be
            // pinned explicitly; Any keeps the payload untyped for our casts.
            val value: CarPropertyValue<Any>? = pm.getProperty<Any>(propertyId, areaId)
            if (value?.status != CarPropertyValue.STATUS_AVAILABLE) return null
            value.value
        } catch (e: Exception) {
            Log.d(TAG, "Read $propertyId (area $areaId) failed: ${e.message}")
            null
        }
    }

    private fun readFloat(propertyId: Int, areaId: Int = AREA_GLOBAL): Float? =
        (readValue(propertyId, areaId) as? Number)?.toFloat()

    private fun readInt(propertyId: Int, areaId: Int = AREA_GLOBAL): Int? =
        (readValue(propertyId, areaId) as? Number)?.toInt()

    private fun readString(propertyId: Int, areaId: Int = AREA_GLOBAL): String? =
        (readValue(propertyId, areaId) as? String)?.takeIf { it.isNotBlank() }

    private fun readBoolean(propertyId: Int, areaId: Int = AREA_GLOBAL): Boolean? =
        readValue(propertyId, areaId) as? Boolean

    // ── Dynamic properties ───────────────────────────────────────────────────

    /**
     * Kilometers per hour, or null when unavailable.
     * PERF_VEHICLE_SPEED is FLOAT in METER_PER_SEC. Negative while rolling
     * backwards, so the magnitude is what we display.
     */
    fun getSpeedKmh(): Float? {
        val metersPerSecond = readFloat(VehiclePropertyIds.PERF_VEHICLE_SPEED) ?: return null
        return kotlin.math.abs(metersPerSecond) * 3.6f
    }

    /** Engine RPM, or null when unavailable. ENGINE_RPM is FLOAT in RPM. */
    fun getRpm(): Float? = readFloat(VehiclePropertyIds.ENGINE_RPM)

    /**
     * Fuel level as a 0.0..1.0 fraction, or null when unavailable.
     * FUEL_LEVEL is FLOAT in MILLILITER, so it only becomes a percentage once
     * divided by INFO_FUEL_CAPACITY (also millilitres).
     */
    fun getFuelLevelFraction(): Float? {
        val currentMl = readFloat(VehiclePropertyIds.FUEL_LEVEL) ?: return null
        val capacityMl = readFloat(VehiclePropertyIds.INFO_FUEL_CAPACITY)
        if (capacityMl == null || capacityMl <= 0f) return null
        return (currentMl / capacityMl).coerceIn(0f, 1f)
    }

    /**
     * Battery state of charge as a 0.0..1.0 fraction, or null when unavailable.
     * EV_BATTERY_LEVEL is FLOAT in WATT_HOUR, relative to
     * INFO_EV_BATTERY_CAPACITY (also watt-hours).
     */
    fun getBatteryLevelFraction(): Float? {
        val currentWh = readFloat(VehiclePropertyIds.EV_BATTERY_LEVEL) ?: return null
        val capacityWh = readFloat(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY)
        if (capacityWh == null || capacityWh <= 0f) return null
        return (currentWh / capacityWh).coerceIn(0f, 1f)
    }

    /**
     * Short display gear ("P", "R", "N", "D", "1".."9"), or null when unavailable.
     *
     * Prefers GEAR_SELECTION (what the driver picked, so it reports "D") over
     * CURRENT_GEAR (the physically engaged ratio, which on the emulator reports
     * 1st..5th and never GEAR_DRIVE). Avoids VehicleGear.toString(), which
     * yields host-facing names like "GEAR_PARK" rather than a dashboard label.
     */
    fun getGearString(): String? {
        val raw = readInt(VehiclePropertyIds.GEAR_SELECTION)
            ?: readInt(VehiclePropertyIds.CURRENT_GEAR)
            ?: return null
        return gearLabel(raw)
    }

    private fun gearLabel(gear: Int): String? = when (gear) {
        VehicleGear.GEAR_UNKNOWN -> null
        VehicleGear.GEAR_PARK -> "P"
        VehicleGear.GEAR_REVERSE -> "R"
        VehicleGear.GEAR_NEUTRAL -> "N"
        VehicleGear.GEAR_DRIVE -> "D"
        VehicleGear.GEAR_FIRST -> "1"
        VehicleGear.GEAR_SECOND -> "2"
        VehicleGear.GEAR_THIRD -> "3"
        VehicleGear.GEAR_FOURTH -> "4"
        VehicleGear.GEAR_FIFTH -> "5"
        VehicleGear.GEAR_SIXTH -> "6"
        VehicleGear.GEAR_SEVENTH -> "7"
        VehicleGear.GEAR_EIGHTH -> "8"
        VehicleGear.GEAR_NINTH -> "9"
        else -> null
    }

    /** True when the engine is running (ignition ON/START), or null if unknown. */
    fun isEngineOn(): Boolean? {
        val ignition = readInt(VehiclePropertyIds.IGNITION_STATE) ?: return null
        return ignition == VehicleIgnitionState.ON || ignition == VehicleIgnitionState.START
    }

    /** Odometer in kilometers, or null when unavailable. PERF_ODOMETER is already KILOMETER. */
    fun getOdometerKm(): Float? = readFloat(VehiclePropertyIds.PERF_ODOMETER)

    /** True when the parking brake is engaged, or null when unavailable. */
    fun isParkingBrakeOn(): Boolean? = readBoolean(VehiclePropertyIds.PARKING_BRAKE_ON)

    /**
     * True when the driver's seatbelt is buckled, or null when unavailable.
     * SEAT_BELT_BUCKLED is BOOLEAN with SEAT area type — it must be read per
     * seat area (ROW_1_LEFT for the driver), never at AREA_GLOBAL.
     */
    fun isDriverSeatbeltOn(): Boolean? =
        readBoolean(VehiclePropertyIds.SEAT_BELT_BUCKLED, AREA_DRIVER_SEAT)

    // ── Vehicle INFO properties ──────────────────────────────────────────────
    // Gated by android.car.permission.CAR_INFO, which is protectionLevel:normal
    // and therefore granted to a third-party app at install. These read real,
    // live VHAL values on the emulator ("Toy Vehicle" / "Speedy Model" / 2023).

    /** Vehicle make, e.g. "Toy Vehicle", or null when unavailable. */
    fun getMake(): String? = readString(VehiclePropertyIds.INFO_MAKE)

    /** Vehicle model, e.g. "Speedy Model", or null when unavailable. */
    fun getModel(): String? = readString(VehiclePropertyIds.INFO_MODEL)

    /** Vehicle VIN, or null. Needs CAR_IDENTIFICATION (signature|privileged). */
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

    /**
     * Subscribes to every property this app is allowed to read, instead of
     * polling getProperty() on a timer. This is how production code consumes
     * CONTINUOUS properties: the VHAL pushes at the requested rate and the
     * framework delivers only real changes for ON_CHANGE properties.
     *
     * [onEvent] fires whenever a value arrives, so callers can forward the new
     * state instead of asking for it. Safe to call repeatedly.
     */
    fun startSubscriptions(onEvent: () -> Unit) {
        ensureConnected()
        val pm = propertyManager ?: run {
            Log.d(TAG, "No car service - cannot subscribe, values stay simulated")
            return
        }
        if (eventCallback != null) return
        this.onEvent = onEvent

        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                if (value.status != CarPropertyValue.STATUS_AVAILABLE) return
                val payload = value.value ?: return
                latest[cacheKey(value.propertyId, value.areaId)] = payload
                this@VehicleHalManager.onEvent?.invoke()
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                Log.d(TAG, "Property error: id=$propertyId area=$areaId")
            }
        }
        eventCallback = callback

        var subscribed = 0
        SUBSCRIPTIONS.forEach { (propertyId, rateHz) ->
            if (!isSupported(propertyId)) return@forEach
            val ok = try {
                pm.registerCallback(callback, propertyId, rateHz)
            } catch (e: Exception) {
                Log.d(TAG, "registerCallback($propertyId) failed: ${e.message}")
                false
            }
            if (ok) subscribed++
            Log.d(TAG, "subscribe $propertyId @ ${rateHz}Hz -> $ok")
        }
        Log.d(TAG, "Subscribed to $subscribed of ${SUBSCRIPTIONS.size} properties")
    }

    fun stopSubscriptions() {
        val pm = propertyManager
        val callback = eventCallback ?: return
        try {
            pm?.unregisterCallback(callback)
        } catch (e: Exception) {
            Log.d(TAG, "unregisterCallback failed: ${e.message}")
        }
        eventCallback = null
        onEvent = null
        latest.clear()
    }

    /**
     * Logs which properties this install can actually read. Handy on the
     * emulator to see at a glance what is live VHAL vs. simulated fallback.
     */
    fun logAvailability() {
        if (!isAvailable) {
            Log.d(TAG, "Availability: car service not connected — all values simulated")
            return
        }
        val props = listOf(
            "PERF_VEHICLE_SPEED" to VehiclePropertyIds.PERF_VEHICLE_SPEED,
            "ENGINE_RPM" to VehiclePropertyIds.ENGINE_RPM,
            "FUEL_LEVEL" to VehiclePropertyIds.FUEL_LEVEL,
            "EV_BATTERY_LEVEL" to VehiclePropertyIds.EV_BATTERY_LEVEL,
            "PERF_ODOMETER" to VehiclePropertyIds.PERF_ODOMETER,
            "GEAR_SELECTION" to VehiclePropertyIds.GEAR_SELECTION,
            "CURRENT_GEAR" to VehiclePropertyIds.CURRENT_GEAR,
            "IGNITION_STATE" to VehiclePropertyIds.IGNITION_STATE,
            "PARKING_BRAKE_ON" to VehiclePropertyIds.PARKING_BRAKE_ON,
            "INFO_MAKE" to VehiclePropertyIds.INFO_MAKE,
            "INFO_MODEL" to VehiclePropertyIds.INFO_MODEL,
            "INFO_MODEL_YEAR" to VehiclePropertyIds.INFO_MODEL_YEAR,
            "INFO_VIN" to VehiclePropertyIds.INFO_VIN,
            "INFO_FUEL_CAPACITY" to VehiclePropertyIds.INFO_FUEL_CAPACITY
        )
        props.forEach { (name, id) ->
            Log.d(TAG, "Availability: %-20s %s".format(
                name, if (isSupported(id)) "LIVE VHAL" else "blocked -> simulated"
            ))
        }
    }

    fun release() {
        stopSubscriptions()
        try {
            car?.disconnect()
            car = null
            propertyManager = null
            supportCache.clear()
        } catch (e: Exception) {
            Log.d(TAG, "Release failed: ${e.message}")
        }
    }
}
