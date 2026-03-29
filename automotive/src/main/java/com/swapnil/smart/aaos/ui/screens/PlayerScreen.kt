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
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.media.MusicData
import com.swapnil.smart.aaos.media.Song
import com.swapnil.smart.aaos.media.SmartMusicService
import com.swapnil.smart.aaos.ui.NavigationCallback
import com.swapnil.smart.aaos.utils.AlbumArtLoader
import com.swapnil.smart.aaos.utils.AlertRepository
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
            invalidate() // 🔥 real-time refresh
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
            val speed = VehicleRepository.getSpeed()
            if (speed <= 2f) {
                mediaController?.transportControls?.skipToNext()
            }
        }
    }

    private fun loadAlbumArt() {
        val songIndex = MusicData.songs.indexOfFirst { it.id == song.id }

        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = if (song.artUrl.isNotEmpty())
                AlbumArtLoader.loadBitmap(song.artUrl)
            else null

            albumArtBitmap = bitmap ?: AlbumArtLoader.generatePlaceholder(
                song.title,
                AlbumArtLoader.getColorForSong(songIndex)
            )
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {

        val paneBuilder = Pane.Builder()


        val songNumber = MusicData.songs.indexOfFirst { it.id == song.id } + 1
        val progressBar = buildProgressBar(currentPositionMs, song.durationMs)
        val progressText = buildProgressText(currentPositionMs, song.durationMs)

        val albumArtIcon = if (albumArtBitmap != null) {
            CarIcon.Builder(IconCompat.createWithBitmap(albumArtBitmap!!)).build()
        } else {
            CarIcon.Builder(
                IconCompat.createWithResource(carContext, android.R.drawable.ic_media_play)
            ).build()
        }

        // 🚨 ALERT SYSTEM
        AlertRepository.currentAlert?.let { alert ->
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("🚨 ${alert.message}")
                    .addText("Check vehicle status")
                    .build()
            )
        }

        // 🎵 SONG INFO
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(song.title)
                .addText("${song.artist} • ${song.album}")
                .addText("$progressBar  $progressText • Track $songNumber of ${MusicData.songs.size}")
                .setImage(albumArtIcon)
                .build()
        )

        // ▶️ PLAY / PAUSE
        val playPauseAction = Action.Builder()
            .setTitle(if (isPlaying) "Pause" else "Play")
            .setOnClickListener {
                if (isPlaying)
                    mediaController?.transportControls?.pause()
                else
                    mediaController?.transportControls?.play()
            }
            .build()

        // ⏭ NEXT
        val nextAction = Action.Builder()
            .setTitle("Next")
            .setOnClickListener {
                val speed = VehicleRepository.getSpeed()
                if (speed <= 2f) {
                    val currentIndex = MusicData.songs.indexOfFirst { it.id == song.id }
                    val nextIndex = (currentIndex + 1) % MusicData.songs.size
                    song = MusicData.songs[nextIndex]
                    currentPositionMs = 0L
                    loadAlbumArt()
                    mediaController?.transportControls?.playFromMediaId(song.id, null)
                    invalidate()
                }
            }
            .build()

        // 🔥 MAX 2 ACTIONS RULE
        paneBuilder.addAction(playPauseAction)
        paneBuilder.addAction(nextAction)

        val pane = paneBuilder.build()

        // 🚗 DRIVE / PARK
        val driveParkAction = Action.Builder()
            .setTitle(
                if ((try { VehicleRepository.getSpeed() } catch (_: Exception) { 0f }) > 2f)
                    "🅿️ Park"
                else "🚗 Drive"
            )
            .setOnClickListener {
                try {
                    val speed = VehicleRepository.getSpeed()
                    if (speed > 2f)
                        VehicleRepository.simulateParked()
                    else
                        VehicleRepository.simulateDriving()
                } catch (_: Exception) {}
                updateCarMovement()
            }
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Now Playing")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(driveParkAction)
                    .build()
            )
            .build()
    }

    private fun buildProgressBar(positionMs: Long, durationMs: Long): String {
        if (durationMs <= 0) return "░░░░░░░░░░░░░░░"
        val totalBlocks = 15
        val filled = (positionMs.toFloat() / durationMs.toFloat() * totalBlocks).toInt()
        return "█".repeat(filled) + "░".repeat(totalBlocks - filled)
    }

    private fun buildProgressText(positionMs: Long, durationMs: Long): String {
        return "${formatTime(positionMs)} / ${formatTime(durationMs)}"
    }

    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}