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

    void simulateDriving(float speedKmh, float rpm, float fuel);

    void simulateParked();
}
