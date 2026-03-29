package com.swapnil.smart.aaos.ui.screens

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.media.MusicData
import com.swapnil.smart.aaos.ui.NavigationCallback
import com.swapnil.smart.aaos.utils.AlbumArtLoader
import com.swapnil.smart.aaos.utils.AlertRepository
import com.swapnil.smart.aaos.vehicle.VehicleRepository
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel

class HomeScreen(carContext: CarContext) : Screen(carContext) {

    // ✅ same ViewModel instance — shared across all screens
    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    init {
        VehicleRepository.connect(carContext)
        AlertRepository.start()

        // ✅ observe instead of Handler polling
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
        val listBuilder = ItemList.Builder()
        val isMoving = viewModel.isCarMoving.value ?: false

        // alert row
        viewModel.currentAlert.value?.let { alert ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🚨 ${alert.message}")
                    .addText("Tap to view details")
                    .setOnClickListener {
                        screenManager.push(DiagnosticsScreen(carContext))
                    }
                    .build()
            )
        }

        // dashboard row
        val dashboardRow = Row.Builder()
            .setTitle("🚗 Vehicle Dashboard")
            .addText("Speed • RPM • Fuel • Gear")
        if (!isMoving) {
            dashboardRow.setOnClickListener {
                screenManager.push(DashboardScreen(carContext))
            }
        }

        // diagnostics row
        val diagnosticsRow = Row.Builder()
            .setTitle("📊 Vehicle Diagnostics")
            .addText("Engine • Battery • Alerts")
            .setOnClickListener {
                screenManager.push(DiagnosticsScreen(carContext))
            }

        listBuilder.addItem(diagnosticsRow.build())
        listBuilder.addItem(dashboardRow.build())

        // music rows
        MusicData.songs.forEachIndexed { index, song ->
            val placeholder = AlbumArtLoader.generatePlaceholder(
                song.title,
                AlbumArtLoader.getColorForSong(index)
            )
            val icon = CarIcon.Builder(
                IconCompat.createWithBitmap(placeholder)
            ).build()

            val rowBuilder = Row.Builder()
                .setTitle(song.title)
                .addText("${song.artist} • ${song.album}")
                .setImage(icon)

            if (!isMoving) {
                rowBuilder.setOnClickListener {
                    screenManager.push(
                        PlayerScreen(carContext, song, {})
                    )
                }
            }
            listBuilder.addItem(rowBuilder.build())
        }

        // drive/park action
        val action = Action.Builder()
            .setTitle(if (isMoving) "🅿️ Park" else "🚗 Drive")
            .setOnClickListener {
                if (isMoving) viewModel.simulateParked()
                else viewModel.simulateDriving()
            }
            .build()

        return ListTemplate.Builder()
            .setTitle(if (isMoving) "🚗 Smart AAOS — Driving" else "🅿️ Smart AAOS — Parked")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(ActionStrip.Builder().addAction(action).build())
            .setSingleList(listBuilder.build())
            .build()
    }
}