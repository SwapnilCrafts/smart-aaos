package com.swapnil.smart.aaos.media

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * Single source of truth for the song list on the automotive side.
 *
 * Starts out holding the bundled [MusicData] list so it is never empty (screens
 * index into it and take `size` modulos), then upgrades to the device's real
 * MediaStore library once [load] runs with READ_MEDIA_AUDIO granted.
 */
object SongRepository {

    private const val TAG = "SmartAAOS_Songs"

    @Volatile
    private var _songs: List<Song> = MusicData.songs

    /** Never empty — falls back to the bundled demo list. */
    val songs: List<Song>
        get() = _songs

    /** True once a real on-device library replaced the bundled list. */
    @Volatile
    var isUsingLocalSongs: Boolean = false
        private set

    /** The audio-read permission appropriate for this API level. */
    val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasAudioPermission(context: Context): Boolean =
        context.checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED

    /**
     * Scans MediaStore and swaps in the device's songs. Safe to call repeatedly
     * (e.g. again after the permission is granted); a failed or empty scan
     * leaves the bundled list in place.
     */
    fun load(context: Context) {
        if (!hasAudioPermission(context)) {
            Log.d(TAG, "Audio permission not granted - keeping ${_songs.size} bundled songs")
            return
        }

        val local = try {
            LocalSongLoader.loadSongs(context)
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore scan failed: ${e.message}")
            emptyList()
        }

        if (local.isNotEmpty()) {
            _songs = local
            isUsingLocalSongs = true
            Log.d(TAG, "Loaded ${local.size} local songs from MediaStore")
        } else {
            _songs = MusicData.songs
            isUsingLocalSongs = false
            Log.d(TAG, "No local songs found - using ${MusicData.songs.size} bundled songs")
        }
    }
}
