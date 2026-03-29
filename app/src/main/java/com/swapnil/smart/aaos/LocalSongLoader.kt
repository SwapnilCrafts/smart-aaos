package com.swapnil.smart.aaos

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore

object LocalSongLoader {

    fun loadSongs(context: Context): List<Song> {
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            val idCol       = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol    = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol     = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                songs.add(
                    Song(
                        id         = it.getLong(idCol).toString(),
                        title      = it.getString(titleCol) ?: "Unknown Title",
                        artist     = it.getString(artistCol) ?: "Unknown Artist",
                        album      = it.getString(albumCol) ?: "Unknown Album",
                        durationMs = it.getLong(durationCol),
                        url        = it.getString(dataCol) ?: continue
                    )
                )
            }
        }

        return songs
    }
}