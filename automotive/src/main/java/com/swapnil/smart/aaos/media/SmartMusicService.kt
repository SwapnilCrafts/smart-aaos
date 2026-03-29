package com.swapnil.smart.aaos.media

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.swapnil.smart.aaos.ui.NavigationCallback

class SmartMusicService : MediaBrowserServiceCompat() {

    private lateinit var session: MediaSessionCompat
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var audioManager: AudioManager        // ✅ add
    private lateinit var audioFocusRequest: AudioFocusRequest // ✅ add
    private var currentIndex = 0

    companion object {
        const val CHANNEL_ID = "smart_aaos_channel"
        const val NOTIFICATION_ID = 1
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // came back from a call or assistant — resume
                exoPlayer.volume = 1f
                exoPlayer.play()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                Log.d("SmartAAOS", "Audio focus gained — resuming")
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // lost focus permanently (another app took over) — stop
                exoPlayer.pause()
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                abandonAudioFocus()
                Log.d("SmartAAOS", "Audio focus lost — pausing")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // lost focus temporarily (phone call, assistant) — pause
                exoPlayer.pause()
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                Log.d("SmartAAOS", "Audio focus lost transient — pausing")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // notification sound etc — lower volume instead of pausing
                exoPlayer.volume = 0.2f
                Log.d("SmartAAOS", "Audio focus duck — lowering volume")
            }
        }
    }
    // ✅ Request audio focus before playing
    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusListener)
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest)
        Log.d("SmartAAOS", "Audio focus request result: $result")
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    // ✅ Abandon focus when stopping
    private fun abandonAudioFocus() {
        if (::audioFocusRequest.isInitialized) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
            Log.d("SmartAAOS", "Audio focus abandoned")
        }
    }
    private val callback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            if (requestAudioFocus()) {      // ✅ only play if focus granted
                exoPlayer.play()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                startForegroundService()
            }
        }

        override fun onPause() {
            exoPlayer.pause()
            stopProgressTimer()
            abandonAudioFocus()             // ✅ release focus on pause
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }

        override fun onStop() {
            exoPlayer.stop()
            stopProgressTimer()
            abandonAudioFocus()             // ✅ release focus on stop
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
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }

        override fun onSkipToQueueItem(queueId: Long) {}
        override fun onCustomAction(action: String?, extras: Bundle?) {}

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            Log.d("SmartAAOS", "onPlayFromSearch: $query")
            if (query.isNullOrEmpty()) { playSong(0); return }
            val index = findSongByQuery(query)
            currentIndex = if (index != -1) index else 0
            playSong(currentIndex)
        }
    }
    // ✅ Smart search — matches title, artist or album
    private fun findSongByQuery(query: String): Int {
        val lowerQuery = query.lowercase().trim()

        Log.d("SmartAAOS", "Searching for: $lowerQuery")
        Log.d("SmartAAOS", "Available songs: ${MusicData.songs.map { it.title }}")

        // First try exact title match
        var index = MusicData.songs.indexOfFirst {
            it.title.lowercase() == lowerQuery
        }
        if (index != -1) return index

        // Then try title contains
        index = MusicData.songs.indexOfFirst {
            it.title.lowercase().contains(lowerQuery)
        }
        if (index != -1) return index

        // Then try artist match
        index = MusicData.songs.indexOfFirst {
            it.artist.lowercase().contains(lowerQuery)
        }
        if (index != -1) return index

        // Then try album match
        index = MusicData.songs.indexOfFirst {
            it.album.lowercase().contains(lowerQuery)
        }
        if (index != -1) return index

        // No match found
        return -1
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Create ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()

        // 2. Listen for ExoPlayer state changes
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        if (exoPlayer.playWhenReady) {
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        }
                    }
                    Player.STATE_ENDED -> {

                        onSkipToNext()
                    }
                    else -> {}
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("SmartAAOS", "Player error: ${error.message}")
                // skip to next song automatically on any error
                onSkipToNext()
            }
        })

        // 3. Create MediaSession
        session = MediaSessionCompat(this, "SmartMusicService")
        sessionToken = session.sessionToken
        session.setCallback(callback)
        session.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        // 4. Create notification channel
        createNotificationChannel()

        // 5. Set first song ready
        updateMetadata(currentIndex)
        updatePlaybackState(PlaybackStateCompat.STATE_NONE)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SmartAAOS", "onStartCommand called action: ${intent?.action}")

        if (intent?.action == "android.media.action.MEDIA_PLAY_FROM_SEARCH") {
            val query = intent.getStringExtra("query")
            Log.d("SmartAAOS", "Voice query received: $query")

            if (!query.isNullOrEmpty()) {
                val songIndex = findSongByQuery(query)
                Log.d("SmartAAOS", "Song index found: $songIndex")

                if (songIndex != -1) {
                    currentIndex = songIndex
                    playSong(currentIndex)

                    // ✅ Static callback instead of broadcast
                    Log.d("SmartAAOS", "Calling navigation callback")
                    val song = MusicData.songs[songIndex]
                    handler.post {
                        NavigationCallback.onPlaySong?.invoke(song)
                    }

                } else {
                    playSong(0)
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }
    // ✅ Core function — loads and plays a song
    private fun playSong(index: Int) {
        val song = MusicData.songs[index]
        val mediaItem = MediaItem.fromUri(song.url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        if (requestAudioFocus()) {          // ✅ always request before playing
            exoPlayer.play()
        }

        updateMetadata(index)
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        startForegroundService()
        startProgressTimer()
    }

    override fun onDestroy() {
        stopProgressTimer()
        abandonAudioFocus()                 // ✅ release on destroy
        exoPlayer.release()
        session.release()
    }

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            // ✅ This runs on main thread — ExoPlayer is happy
            if (::exoPlayer.isInitialized && exoPlayer.isPlaying) {
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            }
            // Schedule next update in 1 second
            handler.postDelayed(this, 1000L)
        }
    }
    private fun startProgressTimer() {
        stopProgressTimer()
        handler.post(progressRunnable)
    }

    private fun stopProgressTimer() {
        handler.removeCallbacks(progressRunnable)
    }
    private fun onSkipToNext() {
        currentIndex = (currentIndex + 1) % MusicData.songs.size
        playSong(currentIndex)
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
            .setState(state, position, 1.0f)  // ✅ position sent here
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
    // ✅ Foreground service keeps music playing in background
    private fun startForegroundService() {
        val song = MusicData.songs[currentIndex]

        // ✅ Play action
        val playPauseAction = if (exoPlayer.isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PLAY
                )
            )
        }

        // ✅ Previous action
        val prevAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous,
            "Previous",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        )

        // ✅ Next action
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            "Next",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .addAction(prevAction)       // ✅ previous button
            .addAction(playPauseAction)  // ✅ play/pause button
            .addAction(nextAction)       // ✅ next button
            // ✅ MediaStyle — this is the key part
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2) // show all 3 in compact
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // ✅ show on lock screen
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart AAOS Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}