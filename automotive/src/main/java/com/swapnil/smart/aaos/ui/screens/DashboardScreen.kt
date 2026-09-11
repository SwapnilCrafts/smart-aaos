package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.ui.GaugeDrawer
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel
import java.util.Locale

/**
 * Full gauge cluster.
 *
 * Uses [GridTemplate] rather than [androidx.car.app.model.ListTemplate]
 * because `Row.setImage()` is an *icon* slot - the host scales whatever you
 * give it down to roughly 40 dp, which turned [GaugeDrawer]'s 360x360 dials
 * into unreadable specks. Grid items with `IMAGE_TYPE_LARGE` render the
 * bitmaps at a size where the needle, arc and bar segments are actually
 * legible.
 *
 * Six gauges is also the practical ceiling for a grid, which happens to be
 * exactly what a cluster needs.
 */
class DashboardScreen(carContext: CarContext) : Screen(carContext) {

    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    private var lastSpeed = Float.MIN_VALUE
    private var lastRpm = Float.MIN_VALUE
    private var lastFuel = Float.MIN_VALUE
    private var lastGear = ""
    private var lastEngineOn: Boolean? = null
    private var lastConnected: Boolean? = null
    private var lastHasAlert: Boolean? = null

    init {
        // Invalidate only on an actual value change. The ViewModel polls every
        // second; rebuilding the template each tick makes the host re-render
        // needlessly and resets scroll position.
        viewModel.speed.observe(this) { if (it != lastSpeed) { lastSpeed = it; invalidate() } }
        viewModel.rpm.observe(this) { if (it != lastRpm) { lastRpm = it; invalidate() } }
        viewModel.fuel.observe(this) { if (it != lastFuel) { lastFuel = it; invalidate() } }
        viewModel.gear.observe(this) { if (it != lastGear) { lastGear = it ?: ""; invalidate() } }
        viewModel.engineOn.observe(this) {
            if (it != lastEngineOn) { lastEngineOn = it; invalidate() }
        }
        viewModel.isConnected.observe(this) {
            if (it != lastConnected) { lastConnected = it; invalidate() }
        }
        viewModel.currentAlert.observe(this) {
            val has = it != null
            if (has != lastHasAlert) { lastHasAlert = has; invalidate() }
        }
    }

    override fun onGetTemplate(): Template {
        if (viewModel.isConnected.value != true) {
            return GridTemplate.Builder()
                .setTitle("Vehicle Dashboard")
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        val speed = viewModel.speed.value ?: 0f
        val rpm = viewModel.rpm.value ?: 0f
        val fuel = viewModel.fuel.value ?: 0f
        val gear = viewModel.gear.value ?: "P"
        val engineOn = viewModel.engineOn.value ?: false
        val odometer = viewModel.odometer.value ?: 0f
        val hasAlert = viewModel.currentAlert.value != null

        // EV_BATTERY_LEVEL is guarded by CAR_ENERGY, which is
        // protectionLevel:dangerous - so this is real once the runtime
        // permission is granted. -1 means the property never reported a value
        // (the emulator's default profile is a fuel vehicle).
        val battery = viewModel.battery.value ?: -1f
        val hasBattery = battery >= 0f
        val rangeKm = (56f * (fuel / 100f)).toInt() * 10

        val gauges = ItemList.Builder()
            .addItem(
                gauge(
                    "Speed",
                    String.format(Locale.US, "%d km/h", speed.toInt()),
                    GaugeDrawer.drawSpeedDial(speed)
                )
            )
            .addItem(
                gauge(
                    "Engine RPM",
                    String.format(Locale.US, "%d rpm", rpm.toInt()),
                    GaugeDrawer.drawRpmArc(rpm)
                )
            )
            .addItem(
                gauge(
                    "Fuel",
                    String.format(Locale.US, "%d%% · ~%d km", fuel.toInt(), rangeKm),
                    GaugeDrawer.drawFuelBar(fuel)
                )
            )
            .addItem(
                gauge(
                    "Gear $gear",
                    if (engineOn) "Engine ON" else "Engine OFF",
                    GaugeDrawer.drawGearStrip(gear, engineOn, hasAlert)
                )
            )
            .addItem(
                gauge(
                    "Battery",
                    if (hasBattery) "${battery.toInt()}%" else "not reported",
                    GaugeDrawer.drawBatteryBar(if (hasBattery) battery else 0f)
                )
            )
            .addItem(
                gauge(
                    "Odometer",
                    String.format(Locale.US, "%.1f km", odometer),
                    GaugeDrawer.drawFuelBar(100f)
                )
            )
            .build()

        return GridTemplate.Builder()
            .setTitle(if (hasAlert) "Dashboard  ·  Check Required" else "Vehicle Dashboard")
            .setHeaderAction(Action.BACK)
            .setItemSize(GridTemplate.ITEM_SIZE_LARGE)
            .setSingleList(gauges)
            .build()
    }

    private fun gauge(title: String, value: String, bitmap: android.graphics.Bitmap) =
        GridItem.Builder()
            .setTitle(title)
            .setText(value)
            .setImage(
                CarIcon.Builder(
                    IconCompat.createWithBitmap(GaugeDrawer.squared(bitmap))
                ).build(),
                GridItem.IMAGE_TYPE_LARGE
            )
            .build()
}
