package com.swapnil.smart.aaos.ui.screens

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.media.MusicData
import com.swapnil.smart.aaos.ui.NavigationCallback
import com.swapnil.smart.aaos.utils.AlertRepository
import com.swapnil.smart.aaos.utils.AlbumArtLoader
import com.swapnil.smart.aaos.utils.VehicleAlert
import com.swapnil.smart.aaos.vehicle.VehicleRepository
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel

class HomeScreen(carContext: CarContext) : Screen(carContext) {

    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    private var previousMoving: Boolean? = null
    private var previousAlert: VehicleAlert? = null

    init {
        VehicleRepository.connect(carContext)
        AlertRepository.start()

        // Invalidate only on actual state TRANSITIONS so the host doesn't
        // rebuild the template every second (which resets list scroll to top).
        viewModel.isCarMoving.observeForever {
            if (it != previousMoving) {
                previousMoving = it
                invalidate()
            }
        }
        viewModel.currentAlert.observeForever {
            if (it != previousAlert) {
                previousAlert = it
                invalidate()
            }
        }

        NavigationCallback.onPlaySong = { song ->
            screenManager.push(PlayerScreen(carContext, song, {}))
        }
        NavigationCallback.onOpenDashboard = {
            if (viewModel.isCarMoving.value != true) {
                screenManager.push(DashboardScreen(carContext))
            }
        }
    }

    override fun onGetTemplate(): Template {
        val isMoving = viewModel.isCarMoving.value ?: false
        Log.d("NavScreen", "HomeScreen onGetTemplate isMoving=$isMoving")

        // ── Navigation section (listed FIRST for the demo) ──────────────────
        val navListBuilder = ItemList.Builder()
        navListBuilder.addItem(
            Row.Builder()
                .setTitle("Navigation")
                .addText("Map  ·  Turn-by-turn  ·  Destinations")
                .setOnClickListener { screenManager.push(NavigationScreen(carContext)) }
                .build()
        )

        // ── Vehicle section ─────────────────────────────────────────────────
        val vehicleListBuilder = ItemList.Builder()

        // Drive / Park toggle as a visible, tappable list row.
        vehicleListBuilder.addItem(
            Row.Builder()
                .setTitle(if (isMoving) "Park" else "Drive")
                .addText(if (isMoving) "Mode: Driving  ·  Tap to park" else "Mode: Parked  ·  Tap to drive")
                .setOnClickListener {
                    if (isMoving) viewModel.simulateParked()
                    else viewModel.simulateDriving()
                }
                .build()
        )
        val dashRowBuilder = Row.Builder()
            .setTitle("Dashboard")
            .addText("Speed  ·  RPM  ·  Fuel  ·  Gear")
        if (!isMoving) {
            dashRowBuilder.setOnClickListener {
                screenManager.push(DashboardScreen(carContext))
            }
        }
        vehicleListBuilder.addItem(dashRowBuilder.build())

        vehicleListBuilder.addItem(
            Row.Builder()
                .setTitle("Diagnostics")
                .addText("Engine  ·  Battery  ·  Alerts")
                .setOnClickListener { screenManager.push(DiagnosticsScreen(carContext)) }
                .build()
        )

        vehicleListBuilder.addItem(
            Row.Builder()
                .setTitle("Music Library")
                .addText("${MusicData.songs.size} tracks")
                .setOnClickListener { screenManager.push(MusicScreen(carContext)) }
                .build()
        )

        // ── Music section ─────────────────────────────────────────────────
        val musicListBuilder = ItemList.Builder()
        MusicData.songs.forEachIndexed { index, song ->
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

            if (!isMoving) {
                rowBuilder.setOnClickListener {
                    screenManager.push(PlayerScreen(carContext, song, {}))
                }
            }
            musicListBuilder.addItem(rowBuilder.build())
        }

        val templateBuilder = ListTemplate.Builder()
            .setTitle(if (isMoving) "Smart AAOS  —  Driving" else "Smart AAOS  —  Parked")
            .setHeaderAction(Action.APP_ICON)

        // Alert section (only when active)
        viewModel.currentAlert.value?.let { alert ->
            val alertList = ItemList.Builder()
                .addItem(
                    Row.Builder()
                        .setTitle(alert.message)
                        .addText("Severity: ${alert.severity.name}  ·  Tap for details")
                        .setOnClickListener { screenManager.push(DiagnosticsScreen(carContext)) }
                        .build()
                )
                .build()
            templateBuilder.addSectionedList(
                SectionedItemList.create(alertList, "Active Alert")
            )
        }

        templateBuilder
            .addSectionedList(
                SectionedItemList.create(navListBuilder.build(), "Navigation")
            )
            .addSectionedList(
                SectionedItemList.create(vehicleListBuilder.build(), "Vehicle")
            )
            .addSectionedList(
                SectionedItemList.create(
                    musicListBuilder.build(),
                    "Music Library  ·  ${MusicData.songs.size} Songs"
                )
            )

        return templateBuilder.build()
    }
}
