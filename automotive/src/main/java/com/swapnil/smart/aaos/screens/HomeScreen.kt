package com.swapnil.smart.aaos.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.AlbumArtLoader
import com.swapnil.smart.aaos.MusicData
import com.swapnil.smart.aaos.NavigationCallback
import com.swapnil.smart.aaos.VehicleSpeedManager

class HomeScreen(carContext: CarContext) : Screen(carContext) {

    private var selectedSongId: String? = null
    private var isCarMoving = false

    init {
        // ✅ Voice navigation callback
        NavigationCallback.onPlaySong = { song ->
            android.util.Log.d("SmartAAOS", "Navigating to: ${song.title}")
            selectedSongId = song.id
            invalidate()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                screenManager.push(PlayerScreen(carContext, song))
            }
        }

        // ✅ Speed lock — update UI when speed changes
        VehicleSpeedManager.init(carContext) { speedKmh ->
            val wasMoving = isCarMoving
            isCarMoving = speedKmh > 2f

            // Only refresh if state changed
            if (wasMoving != isCarMoving) {
                android.util.Log.d("SmartAAOS",
                    if (isCarMoving) "🚗 Car moving — locking UI"
                    else "🅿️ Car parked — unlocking UI"
                )
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    invalidate()
                }
            }
        }
    }

    override fun onGetTemplate(): Template {

        val listBuilder = ItemList.Builder()

        MusicData.songs.forEachIndexed { index, song ->
            val isSelected = song.id == selectedSongId
            val placeholder = AlbumArtLoader.generatePlaceholder(
                song.title,
                AlbumArtLoader.getColorForSong(index)
            )

            val icon = CarIcon.Builder(
                IconCompat.createWithBitmap(placeholder)
            ).build()

            /*val icon = CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext,
                    if (isSelected)
                        android.R.drawable.ic_media_pause
                    else
                        android.R.drawable.ic_media_play
                )
            ).setTint(
                if (isSelected) CarColor.GREEN
                else if (isCarMoving) CarColor.DEFAULT  // ✅ Grey when moving
                else CarColor.BLUE
            ).build()*/

            val rowBuilder = Row.Builder()
                .setTitle(song.title)
                .addText("${song.artist}  •  ${song.album}")
                .addText("Track ${index + 1}  •  ${formatDuration(song.durationMs)}")
                .setImage(icon)

            // ✅ Only allow clicks when parked
            if (!isCarMoving) {
                rowBuilder.setOnClickListener {
                    selectedSongId = song.id
                    invalidate()
                    screenManager.push(PlayerScreen(carContext, song))
                }
            }

            listBuilder.addItem(rowBuilder.build())
        }

        return ListTemplate.Builder()
            .setTitle(
                // ✅ Show driving status in title
                if (isCarMoving) "🚗 Smart AAOS — Driving"
                else "🅿️ Smart AAOS — Parked"
            )
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle(
                                if (isCarMoving) "🅿️ Park"
                                else "🚗 Drive"
                            )
                            .setOnClickListener {
                                if (isCarMoving) {
                                    // Switch to parked
                                    VehicleSpeedManager.simulateSpeed(0f)
                                    isCarMoving = false
                                } else {
                                    // Switch to driving
                                    VehicleSpeedManager.simulateSpeed(60f)
                                    isCarMoving = true
                                }
                                invalidate()
                            }
                            .build()
                    )
                    .build()
            )
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