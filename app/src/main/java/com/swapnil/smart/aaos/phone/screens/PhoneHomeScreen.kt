package com.swapnil.smart.aaos.phone.screens

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
import com.swapnil.smart.aaos.MusicData

class PhoneHomeScreen(carContext: CarContext) : Screen(carContext) {

    private var selectedSongId: String? = null

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        MusicData.songs.forEachIndexed { index, song ->
            val isSelected = song.id == selectedSongId

            val icon = CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext,
                    if (isSelected) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play
                )
            ).setTint(
                if (isSelected) CarColor.GREEN else CarColor.BLUE
            ).build()

            val statusLine = if (isSelected) "Now Playing" else "Track ${index + 1}  ·  ${formatDuration(song.durationMs)}"

            listBuilder.addItem(
                Row.Builder()
                    .setTitle(song.title)
                    .addText("${song.artist}  ·  ${song.album}")
                    .addText(statusLine)
                    .setImage(icon)
                    .setOnClickListener {
                        selectedSongId = song.id
                        invalidate()
                        screenManager.push(PhonePlayerScreen(carContext, song))
                    }
                    .build()
            )
        }

        val shuffleAction = Action.Builder()
            .setTitle("Shuffle")
            .setOnClickListener {
                val randomSong = MusicData.songs.random()
                selectedSongId = randomSong.id
                invalidate()
                screenManager.push(PhonePlayerScreen(carContext, randomSong))
            }
            .build()

        return ListTemplate.Builder()
            .setTitle("Smart AAOS  —  Auto")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(ActionStrip.Builder().addAction(shuffleAction).build())
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
}
