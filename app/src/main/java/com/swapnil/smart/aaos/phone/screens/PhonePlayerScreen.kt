package com.swapnil.smart.aaos.phone.screens

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
import com.swapnil.smart.aaos.PhoneMusicService
import com.swapnil.smart.aaos.Song

class PhonePlayerScreen(
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
            ComponentName(carContext, PhoneMusicService::class.java),
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

        val songNumber = MusicData.songs.indexOfFirst {
            it.id == song.id
        } + 1

        val progressBar = buildProgressBar(currentPositionMs, song.durationMs)
        val progressText = buildProgressText(currentPositionMs, song.durationMs)

        val playPauseAction = Action.Builder()
            .setTitle(if (isPlaying) "Pause" else "Play")
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        if (isPlaying) android.R.drawable.ic_media_pause
                        else android.R.drawable.ic_media_play
                    )
                ).setTint(CarColor.GREEN).build()
            )
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
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        android.R.drawable.ic_media_next
                    )
                ).setTint(CarColor.BLUE).build()
            )
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

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle(song.title)
                    .addText("${song.artist}  •  ${song.album}")
                    .addText("$progressBar  $progressText  •  Track $songNumber of ${MusicData.songs.size}")
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