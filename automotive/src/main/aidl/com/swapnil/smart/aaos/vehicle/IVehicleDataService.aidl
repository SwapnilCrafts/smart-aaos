// IVehicleDataService.aidl
package com.swapnil.smart.aaos.vehicle;

interface IVehicleDataService {

    float getSpeed();

    float getRpm();

    float getFuelLevel();

    String getGear();

    boolean isEngineOn();

    float getOdometer();

    void simulateDriving(float speedKmh, float rpm, float fuel);

    void simulateParked();
}
