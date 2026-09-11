package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import com.swapnil.smart.aaos.vehicle.VehicleRepository
import com.swapnil.smart.aaos.vehicle.VendorProperties
import java.util.Locale

/**
 * Reads and writes the custom vendor properties this project added to the
 * emulator's VHAL.
 *
 * These are not standard Android properties. They are defined in a JSON config
 * installed by tools/install-vendor-properties.sh, in the VENDOR property
 * group (0x20000000) that manufacturers own. Nothing about them exists in the
 * Car API - the app just asks CarPropertyManager for an ID it happens to know.
 *
 * Drive mode is READ_WRITE, so the row is tappable. That is the interesting
 * path: the app writes to the VHAL, the VHAL stores the value, and it comes
 * back as a subscription event rather than a re-read. Everything else in this
 * app only reads.
 *
 * When the JSON config is not installed, every value reads null and the screen
 * says so instead of showing a plausible fake - the point of this screen is to
 * show whether the platform change actually took effect.
 */
class VendorPropertiesScreen(carContext: CarContext) : Screen(carContext) {

    init {
        // Drive mode is subscribed in VehicleHalManager, so a write comes back
        // as an event; refresh the template when any vehicle data arrives.
        VehicleRepository.observe(::onVehicleData)
        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                VehicleRepository.removeObserver(::onVehicleData)
            }
        })
    }

    private fun onVehicleData() {
        invalidate()
    }

    private fun cycleDriveMode() {
        val current = VehicleRepository.getDriveMode()
        if (current == null) {
            CarToast.makeText(
                carContext,
                "Vendor properties not installed on this VHAL",
                CarToast.LENGTH_LONG
            ).show()
            return
        }
        val next = VendorProperties.nextDriveMode(current)
        val ok = VehicleRepository.setDriveMode(next)
        CarToast.makeText(
            carContext,
            if (ok) "Drive mode -> ${VendorProperties.driveModeLabel(next)}"
            else "VHAL rejected the write",
            CarToast.LENGTH_SHORT
        ).show()
        invalidate()
    }

    override fun onGetTemplate(): Template {
        val driveMode = VehicleRepository.getDriveMode()
        val serviceDueKm = VehicleRepository.getServiceDueKm()
        val batteryHealth = VehicleRepository.getBatteryHealth()
        val installed = driveMode != null || serviceDueKm != null || batteryHealth != null

        val values = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Drive Mode")
                    .addText(
                        if (driveMode == null) "unavailable"
                        else "${VendorProperties.driveModeLabel(driveMode)}  ·  tap to change"
                    )
                    .addText("0x21400001  INT32  READ_WRITE")
                    .setOnClickListener { cycleDriveMode() }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Service Due In")
                    .addText(
                        serviceDueKm?.let { String.format(Locale.US, "%.0f km", it) }
                            ?: "unavailable"
                    )
                    .addText("0x21600002  FLOAT  READ")
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Battery Health")
                    .addText(batteryHealth ?: "unavailable")
                    .addText("0x21100003  STRING  READ")
                    .build()
            )
            .build()

        val explanation = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle(if (installed) "Custom VHAL properties" else "Not installed")
                    .addText(
                        if (installed) "Defined in JSON on the device, not in the Car API"
                        else "Run tools/install-vendor-properties.sh"
                    )
                    .addText(
                        if (installed) "Needs CAR_VENDOR_EXTENSION (privileged)"
                        else "Then restart the VHAL service"
                    )
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setHeaderAction(Action.BACK)
            .setTitle("Vendor Properties")
            .addSectionedList(
                SectionedItemList.create(values, "VENDOR group  ·  0x20000000")
            )
            .addSectionedList(
                SectionedItemList.create(explanation, "About")
            )
            .build()
    }
}
