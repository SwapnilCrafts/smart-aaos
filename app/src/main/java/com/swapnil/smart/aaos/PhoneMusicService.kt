package com.swapnil.smart.aaos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class PhoneMusicService : MediaBrowserServiceCompat() {

    private lateinit var session: MediaSessionCompat
    private lateinit var exoPlayer: ExoPlayer
    private var currentIndex = 0

    companion object {
        const val CHANNEL_ID = "smart_aaos_phone_channel"
        const val NOTIFICATION_ID = 2
    }

    private val callback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            exoPlayer.play()
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }

        override fun onPause() {
            exoPlayer.pause()
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }

        override fun onStop() {
            exoPlayer.stop()
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
            stopSelf()
        }

        override fun onSkipToNext() {
            currentIndex = (currentIndex + 1) % MusicData.songs.size
            playSong(currentIndex)
        }

        override fun onSkipToPrevious() {
            currentIndex = if (currentIndex - 1 < 0)
                MusicData.songs.size - 1
            else currentIndex - 1
            playSong(currentIndex)
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            val index = MusicData.songs.indexOfFirst { it.id == mediaId }
            if (index != -1) {
                currentIndex = index
                playSong(currentIndex)
            }
        }

        override fun onSeekTo(position: Long) {
            exoPlayer.seekTo(position)
        }

        override fun onSkipToQueueItem(queueId: Long) {}
        override fun onCustomAction(action: String?, extras: Bundle?) {}
        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            android.util.Log.d("SmartAAOS", "DHU Voice query: $query")

            if (query.isNullOrEmpty()) {
                // ✅ Empty query — play first song
                android.util.Log.d("SmartAAOS", "Empty query — playing first song")
                playSong(0)
                return
            }

            val lowerQuery = query.lowercase().trim()

            // ✅ Search by title first
            var index = MusicData.songs.indexOfFirst {
                it.title.lowercase() == lowerQuery
            }

            // ✅ Then title contains
            if (index == -1) {
                index = MusicData.songs.indexOfFirst {
                    it.title.lowercase().contains(lowerQuery)
                }
            }

            // ✅ Then artist
            if (index == -1) {
                index = MusicData.songs.indexOfFirst {
                    it.artist.lowercase().contains(lowerQuery)
                }
            }

            // ✅ Then album
            if (index == -1) {
                index = MusicData.songs.indexOfFirst {
                    it.album.lowercase().contains(lowerQuery)
                }
            }

            android.util.Log.d("SmartAAOS", "Found song at index: $index")

            if (index != -1) {
                currentIndex = index
                playSong(currentIndex)
            } else {
                // ✅ No match — play first song
                android.util.Log.d("SmartAAOS", "No match — playing first song")
                playSong(0)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        exoPlayer = ExoPlayer.Builder(this).build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    currentIndex = (currentIndex + 1) % MusicData.songs.size
                    playSong(currentIndex)
                }
            }
        })

        session = MediaSessionCompat(this, "PhoneMusicService")
        sessionToken = session.sessionToken
        session.setCallback(callback)
        session.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        createNotificationChannel()
        updateMetadata(currentIndex)
        updatePlaybackState(PlaybackStateCompat.STATE_NONE)
    }

    private fun playSong(index: Int) {
        val song = MusicData.songs[index]
        val mediaItem = MediaItem.fromUri(song.url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        updateMetadata(index)
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        startForegroundService()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val items = MusicData.songs.map { song ->
            val desc = MediaDescriptionCompat.Builder()
                .setMediaId(song.id)
                .setTitle(song.title)
                .setSubtitle(song.artist)
                .build()
            MediaBrowserCompat.MediaItem(
                desc,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }.toMutableList()
        result.sendResult(items)
    }

    private fun updateMetadata(index: Int) {
        val song = MusicData.songs[index]
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.id)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.durationMs)
            .build()
        session.setMetadata(metadata)
    }

    private fun updatePlaybackState(state: Int) {
        val position = if (::exoPlayer.isInitialized)
            exoPlayer.currentPosition else 0L
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, position, 1.0f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()
        session.setPlaybackState(playbackState)
    }

    private fun startForegroundService() {
        val song = MusicData.songs[currentIndex]
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart AAOS Phone Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        exoPlayer.release()
        session.release()
    }
}