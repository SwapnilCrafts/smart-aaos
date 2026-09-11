// IVehicleDataService.aidl
package com.swapnil.smart.aaos.vehicle;

import com.swapnil.smart.aaos.vehicle.IVehicleDataCallback;

interface IVehicleDataService {

    /**
     * Subscribe to pushed updates. The service sends the current state
     * immediately on registration, so a client never has to poll for its
     * initial values.
     */
    void registerCallback(IVehicleDataCallback callback);

    void unregisterCallback(IVehicleDataCallback callback);

    float getSpeed();

    float getRpm();

    float getFuelLevel();

    /**
     * EV_BATTERY_LEVEL as a percentage. Guarded by CAR_ENERGY, the same
     * dangerous permission as FUEL_LEVEL, so it goes live with a runtime grant.
     */
    float getBatteryLevel();

    String getGear();

    boolean isEngineOn();

    float getOdometer();

    // Vehicle INFO properties — readable by a normal app (CAR_INFO, install-time
    // permission), so these return REAL VHAL values, not just simulated ones.
    String   getMake();
    String   getModel();
    String   getVin();
    int      getModelYear();
    float    getFuelCapacityLitres();

    // Custom vendor properties added by this project, gated by
    // CAR_VENDOR_EXTENSION. Return -1 / "" when unavailable, which is the
    // normal case on a stock emulator without the JSON config installed.
    int      getDriveMode();
    boolean  setDriveMode(int mode);
    float    getServiceDueKm();
    String   getBatteryHealth();

    void simulateDriving(float speedKmh, float rpm, float fuel);

    void simulateParked();
}
