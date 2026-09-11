package com.swapnil.smart.aaos.ui.screens

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Tab
import androidx.car.app.model.TabContents
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.media.SongRepository
import com.swapnil.smart.aaos.ui.NavigationCallback
import com.swapnil.smart.aaos.ui.AppIcons
import com.swapnil.smart.aaos.utils.AlertRepository
import com.swapnil.smart.aaos.utils.AlbumArtLoader
import com.swapnil.smart.aaos.utils.CarLocationProvider
import com.swapnil.smart.aaos.utils.VehicleAlert
import com.swapnil.smart.aaos.vehicle.UxRestrictionsRepository
import com.swapnil.smart.aaos.vehicle.VehiclePermissions
import com.swapnil.smart.aaos.vehicle.VehicleRepository
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel
import java.util.Locale

class HomeScreen(carContext: CarContext) : Screen(carContext) {

    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    private var previousMoving: Boolean? = null
    private var previousRestricted: Boolean? = null
    private var previousAlert: VehicleAlert? = null
    private var previousSpeed = Float.MIN_VALUE
    private var previousGeo: String = ""

    private var activeTabId = TAB_DRIVE

    init {
        VehicleRepository.connect(carContext)
        AlertRepository.start()
        UxRestrictionsRepository.start(carContext)
        ensureSongLibrary()
        ensureVehiclePermissions()
        // Start location here as well as in NavigationScreen so the Go tab can
        // show real distances before the map has ever been opened.
        if (CarLocationProvider.hasPermission(carContext)) {
            CarLocationProvider.start(carContext) { invalidate() }
        }

        // Invalidate only on actual state TRANSITIONS so the host doesn't
        // rebuild the template every second (which resets list scroll to top).
        viewModel.isCarMoving.observe(this) {
            if (it != previousMoving) {
                previousMoving = it
                invalidate()
            }
        }
        viewModel.isInteractionRestricted.observe(this) {
            if (it != previousRestricted) {
                previousRestricted = it
                invalidate()
            }
        }
        viewModel.currentAlert.observe(this) {
            if (it != previousAlert) {
                previousAlert = it
                invalidate()
            }
        }
        // Live gauges on the Drive tab.
        viewModel.speed.observe(this) { if (it != previousSpeed) { previousSpeed = it; if (activeTabId == TAB_DRIVE) invalidate() } }
        viewModel.gear.observe(this) { if (it != previousGeo) { previousGeo = it ?: ""; if (activeTabId == TAB_DRIVE) invalidate() } }

        NavigationCallback.onPlaySong = { song ->
            screenManager.push(PlayerScreen(carContext, song, {}))
        }
        NavigationCallback.onOpenDashboard = {
            if (viewModel.isCarMoving.value != true) {
                screenManager.push(DashboardScreen(carContext))
            }
        }
    }

    /**
     * CAR_SPEED and CAR_ENERGY are protectionLevel:dangerous, i.e. ordinary
     * runtime permissions. Without this request they are never granted and the
     * app reads simulated speed and fuel forever, which is exactly what it did
     * before: the manifest declared them, nothing ever asked for them.
     *
     * Once granted, VehicleHalManager's subscriptions pick the properties up on
     * the next connection, so the only thing needed here is the ask.
     */
    private fun ensureVehiclePermissions() {
        val missing = VehiclePermissions.missingRuntime(carContext)
        if (missing.isEmpty()) return
        try {
            carContext.requestPermissions(missing) { approved, rejected ->
                Log.d(
                    "SmartAAOS_Perms",
                    "car permissions approved=${approved.map(VehiclePermissions::shortName)} " +
                        "rejected=${rejected.map(VehiclePermissions::shortName)}"
                )
                if (approved.isNotEmpty()) {
                    // Reconnect so the HAL layer re-evaluates what it can subscribe to.
                    VehicleRepository.connect(carContext)
                    invalidate()
                }
            }
        } catch (e: Exception) {
            // Hosts can refuse to show the dialog unless the app is foregrounded.
            Log.d("SmartAAOS_Perms", "car permission request unavailable: ${e.message}")
        }
    }

    /**
     * The automotive module has no Activity of its own, so READ_MEDIA_AUDIO has
     * to be requested through the car host. Without it MediaStore returns
     * nothing and the bundled demo list stays in place.
     */
    private fun ensureSongLibrary() {
        if (SongRepository.hasAudioPermission(carContext)) {
            SongRepository.load(carContext)
            return
        }
        try {
            carContext.requestPermissions(
                listOf(SongRepository.audioPermission)
            ) { approved, _ ->
                if (approved.contains(SongRepository.audioPermission)) {
                    SongRepository.load(carContext)
                    invalidate()
                }
            }
        } catch (e: Exception) {
            // Some hosts refuse the dialog unless the app is foregrounded.
            Log.d("SmartAAOS_Songs", "Permission request unavailable: ${e.message}")
        }
    }

    override fun onGetTemplate(): Template {
        val isMoving = viewModel.isCarMoving.value ?: false
        Log.d("NavScreen", "HomeScreen onGetTemplate isMoving=$isMoving tab=$activeTabId")

        val callback = object : TabTemplate.TabCallback {
            override fun onTabSelected(selected: String) {
                Log.d("NavScreen", "tab selected -> $selected")
                activeTabId = selected
                invalidate()
            }
        }

        val template = TabTemplate.Builder(callback)
            .setHeaderAction(Action.APP_ICON)
            .setActiveTabContentId(activeTabId)
            .setTabContents(TabContents.Builder(buildTabContent(activeTabId, isMoving)).build())
            .apply {
                addTab(Tab.Builder().setTitle("Drive").setContentId(TAB_DRIVE).setIcon(AppIcons.drive()).build())
                addTab(Tab.Builder().setTitle("Music").setContentId(TAB_MUSIC).setIcon(AppIcons.music()).build())
                addTab(Tab.Builder().setTitle("Go").setContentId(TAB_NAV).setIcon(AppIcons.go()).build())
                addTab(Tab.Builder().setTitle("Info").setContentId(TAB_INFO).setIcon(AppIcons.info()).build())
            }
            .build()
        return template
    }

    // ── Tab content ────────────────────────────────────────────────────────
    private fun buildTabContent(tabId: String, isMoving: Boolean): Template {
        return when (tabId) {
            TAB_DRIVE -> buildDriveTab(isMoving)
            TAB_MUSIC -> buildMusicTab(isMoving)
            TAB_NAV -> buildNavTab()
            else -> buildInfoTab()
        }
    }

    private fun buildDriveTab(isMoving: Boolean): Template {
        val speed = viewModel.speed.value ?: 0f
        val rpm = viewModel.rpm.value ?: 0f
        val fuel = viewModel.fuel.value ?: 0f
        val gear = viewModel.gear.value ?: "P"
        val engineOn = viewModel.engineOn.value ?: false
        val odometer = viewModel.odometer.value ?: 0f

        val statusList = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle(if (isMoving) "Mode: Driving" else "Mode: Parked")
                    .addText(if (isMoving) "Tap to park the vehicle" else "Tap to start driving")
                    .setOnClickListener {
                        if (isMoving) viewModel.simulateParked() else viewModel.simulateDriving()
                    }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Gear  $gear")
                    .addText(if (engineOn) "Engine ON" else "Engine OFF")
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Odometer")
                    .addText(String.format("Total: %.1f km", odometer))
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Live Gauges")
                    .addText(
                        String.format(
                            Locale.US, "%d km/h  ·  %d rpm  ·  %d%% fuel",
                            speed.toInt(), rpm.toInt(), fuel.toInt()
                        )
                    )
                    .addText(
                        if (isMoving) "Park to open the cluster" else "Open the full cluster"
                    )
                    .setOnClickListener {
                        if (!isRestricted()) screenManager.push(DashboardScreen(carContext))
                    }
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("Drive")
            .setHeaderAction(Action.APP_ICON)
            // One section, not two: the host will not split a sectioned list
            // across scroll pages, so a second section left a large gap.
            .setSingleList(statusList)
            .build()
    }


    /** True when interactive rows should be inert: platform restriction or motion. */
    private fun isRestricted(): Boolean =
        viewModel.isInteractionRestricted.value == true

    private fun buildMusicTab(isMoving: Boolean): Template {
        val listBuilder = ItemList.Builder()
        SongRepository.songs.forEachIndexed { index, song ->
            val icon = CarIcon.Builder(
                IconCompat.createWithBitmap(
                    AlbumArtLoader.generatePlaceholder(
                        song.title,
                        AlbumArtLoader.getColorForSong(index)
                    )
                )
            ).build()
            val rowBuilder = Row.Builder()
                .setTitle(song.title)
                .addText("${song.artist}  ·  ${song.album}")
                .setImage(icon)
            if (!isRestricted()) {
                rowBuilder.setOnClickListener {
                    screenManager.push(PlayerScreen(carContext, song, {}))
                }
            }
            listBuilder.addItem(rowBuilder.build())
        }

        return ListTemplate.Builder()
            .setTitle("Music")
            .setHeaderAction(Action.APP_ICON)
            .addSectionedList(
                SectionedItemList.create(listBuilder.build(), "Library  ·  ${SongRepository.songs.size} Songs")
            )
            .build()
    }

    private fun buildNavTab(): Template {
        val destList = ItemList.Builder()
        destList.addItem(
            Row.Builder()
                .setTitle("Open Map")
                .addText("Choose a destination · Turn-by-turn · Draggable map")
                .setOnClickListener { screenManager.push(NavigationScreen(carContext)) }
                .build()
        )
        NavigationScreen.defaultDestinations().forEach { dest ->
            destList.addItem(
                Row.Builder()
                    .setTitle(dest.name)
                    .addText(String.format(Locale.US, "%.1f km", NavigationScreen.distanceKm(dest)))
                    .addText("Preview route · then Start in Maps")
                    .setOnClickListener {
                        screenManager.push(NavigationScreen(carContext).also { it.startNavigation(dest) })
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Navigation")
            .setHeaderAction(Action.APP_ICON)
            .addSectionedList(SectionedItemList.create(destList.build(), "Destinations"))
            .build()
    }

    private fun buildInfoTab(): Template {
        val infoList = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Diagnostics")
                    .addText("Engine  ·  Battery  ·  Alerts")
                    .setOnClickListener { screenManager.push(DiagnosticsScreen(carContext)) }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Dashboard")
                    .addText("Full gauge cluster")
                    .setOnClickListener {
                        if (!isRestricted()) screenManager.push(DashboardScreen(carContext))
                    }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Vehicle Info")
                    .addText("Make  ·  Model  ·  VIN")
                    .setOnClickListener { screenManager.push(DiagnosticsScreen(carContext)) }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Vendor Properties")
                    .addText("Custom VHAL properties  ·  drive mode")
                    .setOnClickListener { screenManager.push(VendorPropertiesScreen(carContext)) }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Simulation")
                    .addText("Trigger overspeed  ·  faults  ·  alerts")
                    .setOnClickListener { screenManager.push(SimulationScreen(carContext)) }
                    .build()
            )

        val templateBuilder = ListTemplate.Builder()
            .setTitle("Info")
            .setHeaderAction(Action.APP_ICON)
            .addSectionedList(SectionedItemList.create(infoList.build(), "Tools"))

        viewModel.currentAlert.value?.let { alert ->
            val alertList = ItemList.Builder()
                .addItem(
                    Row.Builder()
                        .setTitle(alert.message)
                        .addText("Severity: ${alert.severity.name}")
                        .setOnClickListener { screenManager.push(DiagnosticsScreen(carContext)) }
                        .build()
                )
                .build()
            templateBuilder.addSectionedList(SectionedItemList.create(alertList, "Active Alert"))
        }

        return templateBuilder.build()
    }

    companion object {
        const val TAB_DRIVE = "drive"
        const val TAB_MUSIC = "music"
        const val TAB_NAV = "nav"
        const val TAB_INFO = "info"
    }
}
