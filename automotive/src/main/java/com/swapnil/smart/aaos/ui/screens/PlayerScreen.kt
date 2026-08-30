package com.swapnil.smart.aaos.ui.screens

import android.content.ComponentName
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.media.MusicData
import com.swapnil.smart.aaos.media.Song
import com.swapnil.smart.aaos.media.SmartMusicService
import com.swapnil.smart.aaos.ui.NavigationCallback
import com.swapnil.smart.aaos.utils.AlertRepository
import com.swapnil.smart.aaos.utils.AlbumArtLoader
import com.swapnil.smart.aaos.vehicle.VehicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayerScreen(
    carContext: CarContext,
    private var song: Song,
    private val updateCarMovement: () -> Unit
) : Screen(carContext) {

    private var isPlaying = false
    private var currentPositionMs = 0L
    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null
    private var albumArtBitmap: Bitmap? = null

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 1000L)
        }
    }

    init {
        loadAlbumArt()
        handler.post(refreshRunnable)

        mediaBrowser = MediaBrowserCompat(
            carContext,
            ComponentName(carContext, SmartMusicService::class.java),
            object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    mediaController = MediaControllerCompat(carContext, mediaBrowser!!.sessionToken)
                    mediaController?.transportControls?.playFromMediaId(song.id, null)
                    isPlaying = true

                    mediaController?.registerCallback(object : MediaControllerCompat.Callback() {
                        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                            isPlaying = state?.state == PlaybackStateCompat.STATE_PLAYING
                            currentPositionMs = state?.position ?: 0L
                            invalidate()
                        }

                        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                            val newId = metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                            val newSong = MusicData.songs.firstOrNull { it.id == newId }
                            if (newSong != null && newSong.id != song.id) {
                                song = newSong
                                loadAlbumArt()
                                invalidate()
                            }
                        }
                    })
                    invalidate()
                }
            },
            null
        )
        mediaBrowser?.connect()

        NavigationCallback.onPause = {
            mediaController?.transportControls?.pause()
        }
        NavigationCallback.onNext = {
            if (VehicleRepository.getSpeed() <= 2f) {
                mediaController?.transportControls?.skipToNext()
            }
        }
    }

    private fun loadAlbumArt() {
        val songIndex = MusicData.songs.indexOfFirst { it.id == song.id }
        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = if (song.artUrl.isNotEmpty()) AlbumArtLoader.loadBitmap(song.artUrl) else null
            albumArtBitmap = bitmap ?: AlbumArtLoader.generatePlaceholder(
                song.title,
                AlbumArtLoader.getColorForSong(songIndex)
            )
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val paneBuilder = Pane.Builder()

        val currentIndex = MusicData.songs.indexOfFirst { it.id == song.id }
        val songNumber = currentIndex + 1
        val progressBar = buildProgressBar(currentPositionMs, song.durationMs)
        val progressText = buildProgressText(currentPositionMs, song.durationMs)

        val albumArtIcon = if (albumArtBitmap != null) {
            CarIcon.Builder(IconCompat.createWithBitmap(albumArtBitmap!!)).build()
        } else {
            CarIcon.Builder(
                IconCompat.createWithResource(carContext, android.R.drawable.ic_media_play)
            ).build()
        }

        val hasAlert = AlertRepository.currentAlert != null

        // Alert row — only when active (counts toward 2-row limit)
        if (hasAlert) {
            AlertRepository.currentAlert?.let { alert ->
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("Alert: ${alert.message}")
                        .addText("Check vehicle status")
                        .build()
                )
            }
        }

        // Song info row
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(song.title)
                .addText("${song.artist}  ·  ${song.album}")
                .addText("$progressBar  $progressText  ·  Track $songNumber / ${MusicData.songs.size}")
                .setImage(albumArtIcon)
                .build()
        )

        // Up Next row — only when no alert (stays within 2-row limit)
        if (!hasAlert) {
            val nextIndex = (currentIndex + 1) % MusicData.songs.size
            val nextSong = MusicData.songs[nextIndex]
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Up Next")
                    .addText("${nextSong.title}  ·  ${nextSong.artist}")
                    .build()
            )
        }

        // Play / Pause action
        val playPauseAction = Action.Builder()
            .setTitle(if (isPlaying) "Pause" else "Play")
            .setOnClickListener {
                if (isPlaying) mediaController?.transportControls?.pause()
                else mediaController?.transportControls?.play()
            }
            .build()

        // Next action
        val nextAction = Action.Builder()
            .setTitle("Next")
            .setOnClickListener {
                if (VehicleRepository.getSpeed() <= 2f) {
                    val idx = MusicData.songs.indexOfFirst { it.id == song.id }
                    val nextIdx = (idx + 1) % MusicData.songs.size
                    song = MusicData.songs[nextIdx]
                    currentPositionMs = 0L
                    loadAlbumArt()
                    mediaController?.transportControls?.playFromMediaId(song.id, null)
                    invalidate()
                }
            }
            .build()

        paneBuilder.addAction(playPauseAction)
        paneBuilder.addAction(nextAction)

        // Previous action (icon-only — ActionStrip allows only 1 custom-title action)
        val previousAction = Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(carContext, android.R.drawable.ic_media_previous)
                ).build()
            )
            .setOnClickListener {
                if (VehicleRepository.getSpeed() <= 2f) {
                    val idx = MusicData.songs.indexOfFirst { it.id == song.id }
                    val prevIdx = if (idx > 0) idx - 1 else MusicData.songs.size - 1
                    song = MusicData.songs[prevIdx]
                    currentPositionMs = 0L
                    loadAlbumArt()
                    mediaController?.transportControls?.playFromMediaId(song.id, null)
                    invalidate()
                }
            }
            .build()

        // Drive / Park action
        val driveParkAction = Action.Builder()
            .setTitle(
                if (VehicleRepository.getSpeed() > 2f) "Park" else "Drive"
            )
            .setOnClickListener {
                try {
                    if (VehicleRepository.getSpeed() > 2f) VehicleRepository.simulateParked()
                    else VehicleRepository.simulateDriving()
                } catch (_: Exception) {}
                updateCarMovement()
            }
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Now Playing")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(previousAction)
                    .addAction(driveParkAction)
                    .build()
            )
            .build()
    }

    private fun buildProgressBar(positionMs: Long, durationMs: Long): String {
        if (durationMs <= 0) return "░░░░░░░░░░░░░░░"
        val filled = (positionMs.toFloat() / durationMs.toFloat() * 15).toInt()
        return "█".repeat(filled) + "░".repeat(15 - filled)
    }

    private fun buildProgressText(positionMs: Long, durationMs: Long) =
        "${formatTime(positionMs)} / ${formatTime(durationMs)}"

    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
