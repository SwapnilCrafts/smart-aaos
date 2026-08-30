package com.swapnil.smart.aaos.ui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.media.MusicData
import com.swapnil.smart.aaos.utils.AlbumArtLoader
import com.swapnil.smart.aaos.viewmodel.CarViewModelStore
import com.swapnil.smart.aaos.viewmodel.VehicleViewModel

class MusicScreen(carContext: CarContext) : Screen(carContext) {

    private val viewModel = CarViewModelStore.get(VehicleViewModel::class.java)

    init {
        viewModel.isCarMoving.observeForever { invalidate() }
    }

    override fun onGetTemplate(): Template {
        val isMoving = viewModel.isCarMoving.value ?: false

        val listBuilder = ItemList.Builder()
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
            listBuilder.addItem(rowBuilder.build())
        }

        return ListTemplate.Builder()
            .setTitle("Music Library")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
}
