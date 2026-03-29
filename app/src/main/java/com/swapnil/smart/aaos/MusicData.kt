package com.swapnil.smart.aaos

import android.content.Context

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val url: String,
    val artUrl: String = ""  // ✅ Album art URL
)

object MusicData {
    var songs = listOf(
        Song(
            id = "1",
            title = "Kesariya",
            artist = "Arijit Singh",
            album = "Brahmastra",
            durationMs = 268000L,
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            artUrl = "https://picsum.photos/seed/kesariya/200/200"
        ),
        Song(
            id = "2",
            title = "Tum Hi Ho",
            artist = "Arijit Singh",
            album = "Aashiqui 2",
            durationMs = 250000L,
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            artUrl = "https://picsum.photos/seed/tumhiho/200/200"
        ),
        Song(
            id = "3",
            title = "Raataan Lambiyan",
            artist = "Jubin Nautiyal",
            album = "Shershaah",
            durationMs = 232000L,
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            artUrl = "https://picsum.photos/seed/raataan/200/200"
        ),
        Song(
            id = "4",
            title = "Jai Ho",
            artist = "A.R. Rahman",
            album = "Slumdog Millionaire",
            durationMs = 301000L,
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            artUrl = "https://picsum.photos/seed/jaiho/200/200"
        ),
        Song(
            id = "5",
            title = "Dil Dhadakne Do",
            artist = "Pritam",
            album = "ZNMD",
            durationMs = 273000L,
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            artUrl = "https://picsum.photos/seed/dildhadakne/200/200"
        )
    )
    fun init(context: Context) {
        val local = LocalSongLoader.loadSongs(context)
        // ✅ if local songs found use them, else keep the default list above
        if (local.isNotEmpty()) {
            songs = local
        }
        android.util.Log.d("SmartAAOS", "MusicData loaded: ${songs.size} songs")
    }
}