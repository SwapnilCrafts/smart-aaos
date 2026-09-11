// IVehicleDataCallback.aidl
package com.swapnil.smart.aaos.vehicle;

/**
 * Push channel from VehicleDataService to its clients.
 *
 * Declared `oneway` so a slow or dead client can never block the service's
 * Binder thread - the service broadcasts vehicle state from a VHAL event
 * callback, and blocking there would stall the property event stream.
 */
oneway interface IVehicleDataCallback {

    /**
     * Latest vehicle state. Sent once when a client registers, then on every
     * VHAL property event and whenever a simulated value changes.
     */
    void onVehicleData(
        float speedKmh,
        float rpm,
        float fuelPercent,
        String gear,
        boolean engineOn,
        float odometerKm);
}
