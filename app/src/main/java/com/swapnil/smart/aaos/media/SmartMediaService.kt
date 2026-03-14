package com.swapnil.smart.aaos.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.swapnil.smart.aaos.R
import kotlin.jvm.java

class SmartMediaService : MediaBrowserServiceCompat() {

    companion object {
        const val TAG = "SmartMediaService"
        const val ROOT_ID = "SMART_MEDIA_ROOT"
    }

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(
            1,
            NotificationCompat.Builder(this, "media")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Smart AAOS Media")
                .setContentText("Ready")
                .build()
        )


        Log.i(TAG, "MediaBrowserService created")

        mediaSession = MediaSessionCompat(this, "SmartMediaSession")

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                Log.i(TAG, "onPlay() called")
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            }

            override fun onPause() {
                Log.i(TAG, "onPause() called")
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
        })

        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        mediaSession.isActive = true
        sessionToken = mediaSession.sessionToken

        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "media",
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }




    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE
            )
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        Log.i(TAG, "onGetRoot() from $clientPackageName")
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.i(TAG, "onLoadChildren() parentId=$parentId")

        val items = mutableListOf<MediaBrowserCompat.MediaItem>()

        val item = MediaBrowserCompat.MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId("sample_track_1")
                .setTitle("Sample Track")
                .setSubtitle("AAOS Media Test")
                .build(),
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        )

        items.add(item)
        result.sendResult(items)
    }
}
