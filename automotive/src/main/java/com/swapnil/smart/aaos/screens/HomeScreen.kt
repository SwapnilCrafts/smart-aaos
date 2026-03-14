package com.swapnil.smart.aaos.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.MusicData

class HomeScreen(carContext: CarContext) : Screen(carContext) {

    private var selectedSongId: String? = null

    override fun onGetTemplate(): Template {

        val listBuilder = ItemList.Builder()

        MusicData.songs.forEachIndexed { index, song ->
            val isSelected = song.id == selectedSongId

            // ✅ Music note icon for each song
            val icon = CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext,
                    android.R.drawable.ic_media_play
                )
            )
                // Highlight currently playing song
                .setTint(
                    if (isSelected) CarColor.GREEN
                    else CarColor.DEFAULT
                )
                .build()

            listBuilder.addItem(
                Row.Builder()
                    .setTitle(song.title)
                    .addText("${song.artist}  •  ${song.album}")
                    .addText("Track ${index + 1}  •  ${formatDuration(song.durationMs)}")
                    .setImage(icon)
                    .setOnClickListener {
                        selectedSongId = song.id
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
            .setSingleList(listBuilder.build())
            .build()
    }

    // ✅ Format milliseconds to mm:ss
    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}