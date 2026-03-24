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
import com.swapnil.smart.aaos.vehicle.VehicleRepository

class HomeScreen(carContext: CarContext) : Screen(carContext) {

    private var selectedSongId: String? = null
    private var isCarMoving = false // Read from VehicleDataService

    private val handler = Handler(Looper.getMainLooper())

    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateCarMovementState()
            handler.postDelayed(this, 1000L) // Refresh every second
        }
    }


    init {
        // Connect to AIDL service
        try {
            VehicleRepository.connect(carContext)
        } catch (e: Exception) {
            Log.e("SmartAAOS", "Bind failed: ${e.message}")
        }

        // Voice navigation callback
        NavigationCallback.onPlaySong = { song ->
            selectedSongId = song.id
            invalidate()
            handler.post { screenManager.push(PlayerScreen(carContext, song, ::updateCarMovementState)) }
        }
        handler.post(refreshRunnable)
    }

    private fun updateCarMovementState() {
        try {
            val speed = VehicleRepository.getSpeed()
            val wasMoving = isCarMoving
            isCarMoving = speed > 2f

            if (wasMoving != isCarMoving) {
                Log.d("SmartAAOS", if (isCarMoving) "🚗 Driving" else "🅿️ Parked")
                invalidate()
            }
        } catch (_: Exception) {}
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // Dashboard row
        val dashboardRow = Row.Builder()
            .setTitle("🚗 Vehicle Dashboard")
            .addText("Speed • RPM • Fuel • Gear")
        if (!isCarMoving) {
            dashboardRow.setOnClickListener {
                screenManager.push(DashboardScreen(carContext))
            }
        }
        listBuilder.addItem(dashboardRow.build())

        // Music rows
        MusicData.songs.forEachIndexed { index, song ->
            val placeholder = AlbumArtLoader.generatePlaceholder(song.title, AlbumArtLoader.getColorForSong(index))
            val icon = CarIcon.Builder(IconCompat.createWithBitmap(placeholder)).build()

            val rowBuilder = Row.Builder()
                .setTitle(song.title)
                .addText("${song.artist} • ${song.album}")
                .addText("Track ${index + 1} • ${formatDuration(song.durationMs)}")
                .setImage(icon)

            if (!isCarMoving) {
                rowBuilder.setOnClickListener {
                    selectedSongId = song.id
                    invalidate()
                    screenManager.push(PlayerScreen(carContext, song, ::updateCarMovementState))
                }
            }

            listBuilder.addItem(rowBuilder.build())
        }

        // Drive/Park toggle
        val action = Action.Builder()
            .setTitle(if (isCarMoving) "🅿️ Park" else "🚗 Drive")
            .setOnClickListener {
                try {
                    if (isCarMoving) {
                        VehicleRepository.simulateParked()
                    } else {
                        VehicleRepository.simulateDriving()
                    }
                } catch (_: Exception) {}
                updateCarMovementState()
            }
            .build()

        return ListTemplate.Builder()
            .setTitle(if (isCarMoving) "🚗 Smart AAOS — Driving" else "🅿️ Smart AAOS — Parked")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(ActionStrip.Builder().addAction(action).build())
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}