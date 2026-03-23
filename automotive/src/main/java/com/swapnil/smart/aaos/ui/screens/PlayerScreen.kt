package com.swapnil.smart.aaos.ui.screens

import android.R
import android.content.ComponentName
import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.utils.AlbumArtLoader
import com.swapnil.smart.aaos.media.MusicData
import com.swapnil.smart.aaos.media.SmartMusicService
import com.swapnil.smart.aaos.media.Song
import com.swapnil.smart.aaos.vehicle.VehicleSpeedManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayerScreen(
    carContext: CarContext,
    private var song: Song
) : Screen(carContext) {

    private var isPlaying = false
    private var currentPositionMs = 0L
    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null
    private var albumArtBitmap: Bitmap? = null

    init {
        // ✅ Load album art
        loadAlbumArt()

        mediaBrowser = MediaBrowserCompat(
            carContext,
            ComponentName(carContext, SmartMusicService::class.java),
            object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    mediaController = MediaControllerCompat(
                        carContext,
                        mediaBrowser!!.sessionToken
                    )
                    mediaController?.transportControls
                        ?.playFromMediaId(song.id, null)
                    isPlaying = true

                    mediaController?.registerCallback(
                        object : MediaControllerCompat.Callback() {
                            override fun onPlaybackStateChanged(
                                state: PlaybackStateCompat?
                            ) {
                                isPlaying = state?.state ==
                                        PlaybackStateCompat.STATE_PLAYING
                                currentPositionMs = state?.position ?: 0L
                                invalidate()
                            }

                            override fun onMetadataChanged(
                                metadata: MediaMetadataCompat?
                            ) {
                                val newId = metadata?.getString(
                                    MediaMetadataCompat
                                        .METADATA_KEY_MEDIA_ID
                                )
                                val newSong = MusicData.songs
                                    .firstOrNull { it.id == newId }
                                if (newSong != null && newSong.id != song.id) {
                                    song = newSong
                                    loadAlbumArt()
                                    invalidate()
                                }
                            }
                        }
                    )
                    invalidate()
                }
            },
            null
        )
        mediaBrowser?.connect()
    }

    // ✅ Load album art asynchronously
    private fun loadAlbumArt() {
        val songIndex = MusicData.songs.indexOfFirst { it.id == song.id }

        CoroutineScope(Dispatchers.Main).launch {
            // Try loading from URL first
            val bitmap = if (song.artUrl.isNotEmpty()) {
                AlbumArtLoader.loadBitmap(song.artUrl)
            } else null

            // Fall back to generated placeholder
            albumArtBitmap = bitmap ?: AlbumArtLoader.generatePlaceholder(
                song.title,
                AlbumArtLoader.getColorForSong(songIndex)
            )
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {

        val songNumber = MusicData.songs.indexOfFirst {
            it.id == song.id
        } + 1

        val progressBar = buildProgressBar(currentPositionMs, song.durationMs)
        val progressText = buildProgressText(currentPositionMs, song.durationMs)

        // ✅ Album art icon
        val albumArtIcon = albumArtBitmap?.let { bitmap ->
            CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
        } ?: CarIcon.Builder(
            IconCompat.createWithResource(
                carContext,
                R.drawable.ic_media_play
            )
        ).setTint(CarColor.BLUE).build()

        val playPauseIcon = CarIcon.Builder(
            IconCompat.createWithResource(
                carContext,
                if (isPlaying) R.drawable.ic_media_pause
                else R.drawable.ic_media_play
            )
        ).setTint(CarColor.GREEN).build()

        val nextIcon = CarIcon.Builder(
            IconCompat.createWithResource(
                carContext,
                R.drawable.ic_media_next
            )
        ).setTint(CarColor.BLUE).build()

        val playPauseAction = Action.Builder()
            .setTitle(if (isPlaying) "Pause" else "Play")
            .setIcon(playPauseIcon)
            .setOnClickListener {
                if (isPlaying) {
                    mediaController?.transportControls?.pause()
                } else {
                    mediaController?.transportControls?.play()
                }
            }
            .build()

        val nextAction = Action.Builder()
            .setTitle("Next")
            .setIcon(nextIcon)
            .setOnClickListener {
                if (!VehicleSpeedManager.isMoving) {
                    mediaController?.transportControls?.skipToNext()
                    val currentIndex = MusicData.songs.indexOfFirst {
                        it.id == song.id
                    }
                    val nextIndex = (currentIndex + 1) % MusicData.songs.size
                    song = MusicData.songs[nextIndex]
                    currentPositionMs = 0L
                    loadAlbumArt()
                    invalidate()
                }
            }
            .build()

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle(song.title)
                    .addText("${song.artist}  •  ${song.album}")
                    .addText("$progressBar  $progressText  •  Track $songNumber of ${MusicData.songs.size}")
                    // ✅ Album art shown here
                    .setImage(albumArtIcon)
                    .build()
            )
            .addAction(playPauseAction)
            .addAction(nextAction)
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Now Playing")
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle(
                                if (VehicleSpeedManager.isMoving) "🅿️ Park"
                                else "🚗 Drive"
                            )
                            .setOnClickListener {
                                if (VehicleSpeedManager.isMoving) {
                                    VehicleSpeedManager.simulateSpeed(0f)
                                } else {
                                    VehicleSpeedManager.simulateSpeed(60f)
                                }
                                invalidate()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildProgressBar(positionMs: Long, durationMs: Long): String {
        if (durationMs <= 0) return "░░░░░░░░░░░░░░░"
        val progress = positionMs.toFloat() / durationMs.toFloat()
        val totalBlocks = 15
        val filledBlocks = (progress * totalBlocks).toInt()
        val emptyBlocks = totalBlocks - filledBlocks
        return "${"█".repeat(filledBlocks)}${"░".repeat(emptyBlocks)}"
    }

    private fun buildProgressText(positionMs: Long, durationMs: Long): String {
        return "${formatTime(positionMs)} / ${formatTime(durationMs)}"
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}