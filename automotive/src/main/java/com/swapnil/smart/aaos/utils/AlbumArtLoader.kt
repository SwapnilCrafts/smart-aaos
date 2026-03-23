package com.swapnil.smart.aaos.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object AlbumArtLoader {

    // ✅ Cache loaded bitmaps
    private val cache = mutableMapOf<String, Bitmap>()

    // ✅ Load album art from URL
    suspend fun loadBitmap(url: String): Bitmap? {
        // Return cached if available
        cache[url]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeStream(
                    URL(url).openStream()
                )
                if (bitmap != null) {
                    cache[url] = bitmap
                }
                bitmap
            } catch (e: Exception) {
                Log.d("SmartAAOS", "Album art load failed: ${e.message}")
                null
            }
        }
    }

    // ✅ Generate colored placeholder with song initial
    fun generatePlaceholder(
        songTitle: String,
        color: Int = Color.parseColor("#185FA5")
    ): Bitmap {
        val size = 200
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background color
        val bgPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // First letter of song title
        val textPaint = Paint().apply {
            this.color = Color.WHITE
            textSize = 80f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val letter = songTitle.firstOrNull()?.uppercase() ?: "♪"
        canvas.drawText(
            letter,
            size / 2f,
            size / 2f + 30f,
            textPaint
        )

        return bitmap
    }

    // ✅ Different color for each song
    fun getColorForSong(index: Int): Int {
        val colors = listOf(
            Color.parseColor("#185FA5"),  // Blue
            Color.parseColor("#1D9E75"),  // Teal
            Color.parseColor("#D85A30"),  // Coral
            Color.parseColor("#BA7517"),  // Amber
            Color.parseColor("#993556"),  // Pink
        )
        return colors[index % colors.size]
    }
}