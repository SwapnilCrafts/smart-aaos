package com.swapnil.smart.aaos.ui.screens

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
import com.swapnil.smart.aaos.vehicle.VehicleRepository
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel

class HomeScreen(carContext: CarContext) : Screen(carContext) {

    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    init {
        VehicleRepository.connect(carContext)
        AlertRepository.start()

        viewModel.isCarMoving.observeForever { invalidate() }
        viewModel.currentAlert.observeForever { invalidate() }

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
                .addText("Track ${index + 1}  ·  ${formatDuration(song.durationMs)}")
                .setImage(icon)

            if (!isMoving) {
                rowBuilder.setOnClickListener {
                    screenManager.push(PlayerScreen(carContext, song, {}))
                }
            }
            musicListBuilder.addItem(rowBuilder.build())
        }

        // ── Vehicle section ───────────────────────────────────────────────
        val vehicleListBuilder = ItemList.Builder()

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

        // ── Drive / Park action ───────────────────────────────────────────
        val driveAction = Action.Builder()
            .setTitle(if (isMoving) "Park" else "Drive")
            .setOnClickListener {
                if (isMoving) viewModel.simulateParked()
                else viewModel.simulateDriving()
            }
            .build()

        val templateBuilder = ListTemplate.Builder()
            .setTitle(if (isMoving) "Smart AAOS  —  Driving" else "Smart AAOS  —  Parked")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(ActionStrip.Builder().addAction(driveAction).build())

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
                SectionedItemList.create(
                    musicListBuilder.build(),
                    "Music Library  ·  ${MusicData.songs.size} Songs"
                )
            )
            .addSectionedList(
                SectionedItemList.create(vehicleListBuilder.build(), "Vehicle")
            )

        return templateBuilder.build()
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
}
