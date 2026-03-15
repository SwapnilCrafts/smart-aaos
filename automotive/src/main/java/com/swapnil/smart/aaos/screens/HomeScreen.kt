package com.swapnil.smart.aaos.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.MusicData
import com.swapnil.smart.aaos.NavigationCallback

class HomeScreen(carContext: CarContext) : Screen(carContext) {

    private var selectedSongId: String? = null
    private var isLoading = false

    private val voiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val songId = intent?.getStringExtra("song_id") ?: return
            val song = MusicData.songs.firstOrNull { it.id == songId } ?: return

            // ✅ Navigate to PlayerScreen automatically
            selectedSongId = songId
            invalidate()
            screenManager.push(PlayerScreen(carContext, song))
        }
    }
    init {
        // ✅ Static callback — no broadcast needed
        NavigationCallback.onPlaySong = { song ->
            android.util.Log.d("SmartAAOS", "Navigating to: ${song.title}")
            selectedSongId = song.id
            invalidate()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                screenManager.push(PlayerScreen(carContext, song))
            }
        }
    }
    override fun onGetTemplate(): Template {

        val listBuilder = ItemList.Builder()

        MusicData.songs.forEachIndexed { index, song ->
            val isSelected = song.id == selectedSongId

            val icon = CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext,
                    if (isSelected)
                        android.R.drawable.ic_media_pause
                    else
                        android.R.drawable.ic_media_play
                )
            ).setTint(
                if (isSelected) CarColor.GREEN
                else CarColor.BLUE
            ).build()

            listBuilder.addItem(
                Row.Builder()
                    .setTitle(song.title)
                    .addText("${song.artist}  •  ${song.album}")
                    .addText("Track ${index + 1}  •  ${formatDuration(song.durationMs)}")
                    .setImage(icon)
                    .setOnClickListener {
                        selectedSongId = song.id
                        isLoading = false
                        invalidate()
                        screenManager.push(
                            PlayerScreen(carContext, song)
                        )
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("🎵 Smart AAOS")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("🎤 Jai Ho")
                            .setOnClickListener {
                                val index = MusicData.songs.indexOfFirst {
                                    it.title.lowercase().contains("Jai Ho")
                                }
                                if (index != -1) {
                                    selectedSongId = MusicData.songs[index].id
                                    invalidate()
                                    screenManager.push(
                                        PlayerScreen(
                                            carContext,
                                            MusicData.songs[index]
                                        )
                                    )
                                }
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