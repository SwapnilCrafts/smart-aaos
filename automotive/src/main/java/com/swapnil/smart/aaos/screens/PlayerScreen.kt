package com.swapnil.smart.aaos.screens

import android.content.ComponentName
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.swapnil.smart.aaos.MusicData
import com.swapnil.smart.aaos.SmartMusicService
import com.swapnil.smart.aaos.Song

class PlayerScreen(
    carContext: CarContext,
    private var song: Song
) : Screen(carContext) {

    private var isPlaying = false
    private var currentPositionMs = 0L
    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null

    init {
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

                            // ✅ Update UI when voice command changes song
                            override fun onMetadataChanged(
                                metadata: android.support.v4.media.MediaMetadataCompat?
                            ) {
                                val newId = metadata?.getString(
                                    android.support.v4.media.MediaMetadataCompat
                                        .METADATA_KEY_MEDIA_ID
                                )
                                val newSong = MusicData.songs
                                    .firstOrNull { it.id == newId }
                                if (newSong != null) {
                                    song = newSong
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

    override fun onGetTemplate(): Template {

        // ✅ Build progress bar string
        val progressText = buildProgressText(
            currentPositionMs,
            song.durationMs
        )

        // ✅ Build visual progress bar
        val progressBar = buildProgressBar(
            currentPositionMs,
            song.durationMs
        )

        val playPauseIcon = CarIcon.Builder(
            IconCompat.createWithResource(
                carContext,
                if (isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        ).setTint(CarColor.GREEN).build()

        val nextIcon = CarIcon.Builder(
            IconCompat.createWithResource(
                carContext,
                android.R.drawable.ic_media_next
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
                mediaController?.transportControls?.skipToNext()
                val currentIndex = MusicData.songs.indexOfFirst {
                    it.id == song.id
                }
                val nextIndex = (currentIndex + 1) % MusicData.songs.size
                song = MusicData.songs[nextIndex]
                currentPositionMs = 0L
                invalidate()
            }
            .build()

        val songNumber = MusicData.songs.indexOfFirst {
            it.id == song.id
        } + 1

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle(song.title)
                    // ✅ Line 1 — artist + progress bar
                    .addText("${song.artist}  •  ${song.album}")
                    // ✅ Line 2 — time + track number (max 2 lines!)
                    .addText("$progressBar  ${buildProgressText(currentPositionMs, song.durationMs)}  •  Track $songNumber of ${MusicData.songs.size}")
                    .setImage(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext,
                                android.R.drawable.ic_media_play
                            )
                        ).setTint(
                            if (isPlaying) CarColor.GREEN
                            else CarColor.YELLOW
                        ).build()
                    )
                    .build()
            )
            .addAction(playPauseAction)
            .addAction(nextAction)
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Now Playing")
            .setHeaderAction(Action.BACK)
            .build()
    }

    // ✅ Builds "1:23 / 4:28" text
    private fun buildProgressText(
        positionMs: Long,
        durationMs: Long
    ): String {
        return "${formatTime(positionMs)} / ${formatTime(durationMs)}"
    }

    // ✅ Builds visual progress bar like "████░░░░░░ 45%"
    private fun buildProgressBar(
        positionMs: Long,
        durationMs: Long
    ): String {
        if (durationMs <= 0) return "░░░░░░░░░░"
        val progress = (positionMs.toFloat() / durationMs.toFloat())
        val totalBlocks = 20
        val filledBlocks = (progress * totalBlocks).toInt()
        val emptyBlocks = totalBlocks - filledBlocks
        val percent = (progress * 100).toInt()
        return "${"█".repeat(filledBlocks)}${"░".repeat(emptyBlocks)} $percent%"
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
