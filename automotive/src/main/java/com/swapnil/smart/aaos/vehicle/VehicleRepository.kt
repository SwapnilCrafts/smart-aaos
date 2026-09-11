package com.swapnil.smart.aaos.vehicle

import android.content.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

/** Immutable view of vehicle state, as pushed by [VehicleDataService]. */
data class VehicleSnapshot(
    val speedKmh: Float = 0f,
    val rpm: Float = 0f,
    val fuelPercent: Float = 0f,
    val gear: String = "P",
    val engineOn: Boolean = false,
    val odometerKm: Float = 0f
)

object VehicleRepository {

    private const val TAG = "VehicleRepository"

    private var vehicleService: IVehicleDataService? = null
    var isConnected: Boolean = false
        private set

    private var isBinding = false

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Latest pushed state. Updated from a Binder thread, read from the main
     * thread, hence @Volatile.
     */
    @Volatile
    var snapshot: VehicleSnapshot = VehicleSnapshot()
        private set

    private val dataListeners = mutableListOf<() -> Unit>()
    private val connectionListeners = mutableListOf<() -> Unit>()

    /**
     * Receives pushed vehicle state. Called on a Binder thread, so listeners
     * are dispatched to the main thread - they touch LiveData and screen
     * state.
     */
    private val dataCallback = object : IVehicleDataCallback.Stub() {
        override fun onVehicleData(
            speedKmh: Float,
            rpm: Float,
            fuelPercent: Float,
            gear: String?,
            engineOn: Boolean,
            odometerKm: Float
        ) {
            snapshot = VehicleSnapshot(
                speedKmh = speedKmh,
                rpm = rpm,
                fuelPercent = fuelPercent,
                gear = gear ?: "P",
                engineOn = engineOn,
                odometerKm = odometerKm
            )
            mainHandler.post { dataListeners.toList().forEach { it() } }
        }
    }

    /** Notified on every pushed update. */
    fun observe(listener: () -> Unit) {
        dataListeners.add(listener)
    }

    fun removeObserver(listener: () -> Unit) {
        dataListeners.remove(listener)
    }

    /** Notified when the AIDL connection is established or lost. */
    fun observeConnection(listener: () -> Unit) {
        connectionListeners.add(listener)
    }

    fun removeConnectionObserver(listener: () -> Unit) {
        connectionListeners.remove(listener)
    }

    // ✅ Service connection (ONLY ONE in whole app)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "AIDL connected")
            val remote = IVehicleDataService.Stub.asInterface(service)
            vehicleService = remote
            isConnected = true
            // Subscribe for pushes; the service replies with current state
            // immediately, so there is nothing to poll for.
            try {
                remote.registerCallback(dataCallback)
            } catch (e: Exception) {
                Log.d(TAG, "registerCallback failed: ${e.message}")
            }
            mainHandler.post { connectionListeners.toList().forEach { it() } }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "AIDL disconnected")
            vehicleService = null
            isConnected = false
            mainHandler.post { connectionListeners.toList().forEach { it() } }
        }
    }

    // ✅ Call this ONCE (from first screen)
    fun connect(context: Context) {
        if (isConnected || isBinding) return

        try {
            val intent = Intent(context, VehicleDataService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            isBinding = true
            Log.d(TAG, "🔄 Binding to VehicleDataService...")
        } catch (e: Exception) {
            Log.d(TAG, "Bind failed: ${e.message}")
        }
    }

    // ✅ Access methods (safe calls)

    fun getSpeed(): Float {
        return try { vehicleService?.getSpeed() ?: 0f } catch (e: Exception) { 0f }
    }

    fun getRpm(): Float {
        return try { vehicleService?.getRpm() ?: 0f } catch (e: Exception) { 0f }
    }

    fun getFuel(): Float {
        return try { vehicleService?.getFuelLevel() ?: 0f } catch (e: Exception) { 0f }
    }

    fun getGear(): String {
        return try { vehicleService?.getGear() ?: "P" } catch (e: Exception) { "P" }
    }

    fun isEngineOn(): Boolean {
        return try { vehicleService?.isEngineOn() ?: false } catch (e: Exception) { false }
    }

    fun getOdometer(): Float {
        return try { vehicleService?.getOdometer() ?: 0f } catch (e: Exception) { 0f }
    }

    fun getMake(): String {
        return try { vehicleService?.getMake() ?: "Smart AAOS EV" } catch (e: Exception) { "Smart AAOS EV" }
    }

    fun getModel(): String {
        return try { vehicleService?.getModel() ?: "Twin Turbo" } catch (e: Exception) { "Twin Turbo" }
    }

    fun getVin(): String {
        return try { vehicleService?.getVin() ?: "" } catch (e: Exception) { "" }
    }

    fun getModelYear(): Int {
        return try { vehicleService?.getModelYear() ?: 0 } catch (e: Exception) { 0 }
    }

    fun getFuelCapacityLitres(): Float {
        return try { vehicleService?.getFuelCapacityLitres() ?: -1f } catch (e: Exception) { -1f }
    }

    fun simulateDriving() {
        try {
            vehicleService?.simulateDriving(60f, 2000f, 70f)
        } catch (e: Exception) {
            Log.d(TAG, "Simulate driving error: ${e.message}")
        }
    }
    fun simulateDriving1() {
        try {
            vehicleService?.simulateDriving(
                120f,   // 🚗 High speed → Overspeed
                5500f,  // 🧠 High RPM → Engine fault
                8f      // ⛽ Low fuel → Fuel fault
            )
        } catch (e: Exception) {
            Log.d(TAG, "Simulate driving error: ${e.message}")
        }
    }

    fun simulateNormalDriving() {
        vehicleService?.simulateDriving(50f, 2000f, 60f)
    }

    fun simulateOverspeed() {
        vehicleService?.simulateDriving(120f, 3000f, 60f)
    }

    fun simulateEngineFault() {
        vehicleService?.simulateDriving(60f, 5500f, 60f)
    }

    fun simulateLowFuel() {
        vehicleService?.simulateDriving(40f, 2000f, 8f)
    }

    fun simulateCriticalAll() {
        vehicleService?.simulateDriving(140f, 6000f, 5f)
    }
    fun simulateParked() {
        try {
            vehicleService?.simulateParked()
        } catch (e: Exception) {
            Log.d(TAG, "Simulate parked error: ${e.message}")
        }
    }
    fun disconnect(context: Context) {
        try {
            if (isConnected || isBinding) {
                try {
                    vehicleService?.unregisterCallback(dataCallback)
                } catch (e: Exception) {
                    Log.d(TAG, "unregisterCallback failed: ${e.message}")
                }
                context.unbindService(serviceConnection)
                vehicleService = null
                isConnected = false
                isBinding = false
                Log.d(TAG, "✅ Disconnected from VehicleDataService")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Disconnect error: ${e.message}")
        }
    }
}