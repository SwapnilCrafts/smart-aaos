package com.swapnil.smart.aaos.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat

object AppIcons {

    private fun carIcon(size: Int, draw: (Canvas, Paint) -> Unit): CarIcon {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = size / 9f
            strokeCap = Paint.Cap.ROUND
        }
        draw(canvas, paint)
        return CarIcon.Builder(IconCompat.createWithBitmap(bmp)).build()
    }

    fun drive(): CarIcon = carIcon(96) { c, p ->
        c.drawCircle(48f, 48f, 34f, p)
        c.drawLine(48f, 14f, 48f, 36f, p)
        c.drawLine(48f, 82f, 48f, 60f, p)
        c.drawLine(14f, 48f, 36f, 48f, p)
        c.drawLine(82f, 48f, 60f, 48f, p)
    }

    fun music(): CarIcon = carIcon(96) { c, p ->
        c.drawCircle(36f, 66f, 16f, p)
        c.drawCircle(66f, 58f, 16f, p)
        c.drawLine(52f, 66f, 52f, 26f, p)
        c.drawLine(52f, 26f, 78f, 20f, p)
        c.drawLine(78f, 20f, 78f, 58f, p)
    }

    fun go(): CarIcon = carIcon(96) { c, p ->
        val path = android.graphics.Path().apply {
            moveTo(48f, 82f)
            lineTo(28f, 44f)
            cubicTo(28f, 30f, 37f, 20f, 48f, 20f)
            cubicTo(59f, 20f, 68f, 30f, 68f, 44f)
            close()
        }
        p.style = Paint.Style.STROKE
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
        c.drawCircle(48f, 44f, 9f, p)
    }

    fun info(): CarIcon = carIcon(96) { c, p ->
        p.style = Paint.Style.FILL
        c.drawCircle(48f, 28f, 9f, p)
        p.style = Paint.Style.STROKE
        c.drawLine(48f, 48f, 48f, 78f, p)
    }
}