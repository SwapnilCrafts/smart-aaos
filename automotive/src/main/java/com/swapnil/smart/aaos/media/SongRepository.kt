package com.swapnil.smart.aaos.media

import android.content.Context

object SongRepository {
    private var _songs: List<Song> = emptyList()

    val songs: List<Song>
        get() = _songs

    fun load(context: Context) {
        val localSongs = LocalSongLoader.loadSongs(context)

        _songs = if (localSongs.isNotEmpty()) {
            // ✅ Use real device songs
            localSongs
        } else {
            // ✅ Fallback to hardcoded songs if no local songs found
            MusicData.songs
        }
    }
}