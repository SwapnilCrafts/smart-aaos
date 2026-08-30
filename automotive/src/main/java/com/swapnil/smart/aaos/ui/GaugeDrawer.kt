package com.swapnil.smart.aaos.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders automotive gauge graphics (speed dial, RPM arc, fuel bar, battery
 * bar) to [Bitmap]s so they can be shown as [androidx.car.app.model.Row] images
 * inside Car App Library templates. This gives a polished instrument-cluster
 * look without requiring the (unavailable-in-1.4.0) custom-window API.
 */
object GaugeDrawer {

    private fun newCanvas(size: Int): Pair<android.graphics.Bitmap, Canvas> {
        val bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        return bm to Canvas(bm)
    }

    // ── Speed dial (0-200 km/h) ────────────────────────────────────────────
    fun drawSpeedDial(speedKmh: Float, size: Int = 360): Bitmap {
        val (bm, c) = newCanvas(size)
        val cx = size / 2f
        val cy = size / 2f
        val radius = size * 0.40f

        // Background disc
        c.drawCircle(cx, cy, radius, paint(Color.rgb(16, 22, 30), Paint.Style.FILL))

        // Gauge arc (sweep 0..1 of speed)
        val frac = (speedKmh / 200f).coerceIn(0f, 1f)
        val arcPaint = strokePaint(Color.rgb(0, 168, 255), size * 0.035f)
        c.drawArc(
            RectF(cx - radius, cy - radius, cx + radius, cy + radius),
            135f, 270f * frac + 0.5f, false, arcPaint
        )

        // Tick marks
        val tickPaint = strokePaint(Color.rgb(120, 140, 160), size * 0.012f)
        for (i in 0..10) {
            val a = Math.toRadians(135.0 + i * 27.0)
            val r1 = radius * 0.72f
            val r2 = if (i % 2 == 0) radius * 0.84f else radius * 0.79f
            c.drawLine(
                cx + r1 * cos(a).toFloat(), cy + r1 * sin(a).toFloat(),
                cx + r2 * cos(a).toFloat(), cy + r2 * sin(a).toFloat(), tickPaint
            )
        }

        // Needle
        val needlePaint = strokePaint(Color.rgb(255, 80, 80), size * 0.02f)
        val a = Math.toRadians(135.0 + 270.0 * frac)
        c.drawLine(
            cx, cy,
            cx + radius * 0.62f * cos(a).toFloat(),
            cy + radius * 0.62f * sin(a).toFloat(), needlePaint
        )

        // Center cap
        c.drawCircle(cx, cy, size * 0.035f, paint(Color.rgb(255, 80, 80), Paint.Style.FILL))

        // Numeric readout
        val text = speedKmh.toInt().toString()
        val tp = textPaint(Color.WHITE, size * 0.12f)
        val baseline = cy + radius * 0.15f
        c.drawText(text, cx, baseline + tp.textSize * 0.3f, tp)
        c.drawText(
            "km/h", cx, baseline + tp.textSize * 1.35f,
            textPaint(Color.rgb(140, 160, 180), size * 0.05f)
        )
        return bm
    }

    // ── RPM arc (0-8000) ────────────────────────────────────────────────────
    fun drawRpmArc(rpm: Float, size: Int = 360): Bitmap {
        val (bm, c) = newCanvas(size)
        val side = size * 0.10f
        val stroke = size * 0.05f
        val rect = RectF(side, side, size - side, size - side)

        val frac = (rpm / 8000f).coerceIn(0f, 1f)
        val redLine = 6000f

        // Background track
        c.drawArc(rect, 180f, 180f, false, strokePaint(Color.rgb(40, 48, 58), stroke))

        // Active arc, colored by zone
        val color = when {
            rpm > redLine -> Color.rgb(255, 60, 60)
            rpm > 4000    -> Color.rgb(255, 170, 40)
            else          -> Color.rgb(0, 210, 120)
        }
        c.drawArc(rect, 180f, 180f * frac, false, strokePaint(color, stroke))

        // Redline zone indicator
        val redStart = 180f + 180f * (redLine / 8000f)
        c.drawArc(rect, redStart, 180f - redStart, false, strokePaint(Color.rgb(180, 40, 40), stroke))

        val tp = textPaint(Color.WHITE, size * 0.12f)
        c.drawText(rpm.toInt().toString(), size / 2f, size * 0.5f, tp)
        c.drawText(
            "RPM", size / 2f, size * 0.5f + tp.textSize * 1.1f,
            textPaint(Color.rgb(140, 160, 180), size * 0.05f)
        )
        return bm
    }

    // ── Fuel bar (0-100%) ───────────────────────────────────────────────────
    fun drawFuelBar(fuel: Float, size: Int = 340): Bitmap {
        val h = (size * 0.20f).toInt().coerceAtLeast(48)
        val bm = Bitmap.createBitmap(size, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val frac = (fuel / 100f).coerceIn(0f, 1f)

        val color = when {
            fuel < 15  -> Color.rgb(255, 70, 70)
            fuel < 30  -> Color.rgb(255, 180, 40)
            else       -> Color.rgb(0, 200, 120)
        }
        val inset = size * 0.02f
        drawRoundBar(c, inset, h * 0.28f, size - inset, h * 0.72f, Color.rgb(40, 48, 58), size * 0.03f)
        drawRoundBar(c, inset, h * 0.28f, inset + (size - 2 * inset) * frac, h * 0.72f, color, size * 0.03f)

        c.drawText(
            "Fuel  ${fuel.toInt()}%", size * 0.06f, h * 0.85f,
            textPaint(Color.WHITE, h * 0.5f)
        )
        return bm
    }

    // ── Battery bar (0-100%) ────────────────────────────────────────────────
    fun drawBatteryBar(level: Float, size: Int = 340): Bitmap {
        val h = (size * 0.20f).toInt().coerceAtLeast(48)
        val bm = Bitmap.createBitmap(size, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val frac = (level / 100f).coerceIn(0f, 1f)
        val color = if (level < 20) Color.rgb(255, 70, 70) else Color.rgb(80, 180, 255)
        val inset = size * 0.02f
        drawRoundBar(c, inset, h * 0.28f, size - inset, h * 0.72f, Color.rgb(40, 48, 58), size * 0.03f)
        drawRoundBar(c, inset, h * 0.28f, inset + (size - 2 * inset) * frac, h * 0.72f, color, size * 0.03f)
        c.drawText(
            "Battery  ${level.toInt()}%", size * 0.06f, h * 0.85f,
            textPaint(Color.WHITE, h * 0.5f)
        )
        return bm
    }

    private fun drawRoundBar(
        c: Canvas, l: Float, t: Float, r: Float, b: Float,
        color: Int, radius: Float
    ) {
        if (r <= l + 2f) return
        c.drawRoundRect(RectF(l, t, r, b), radius, radius, paint(color, Paint.Style.FILL))
    }

    // ── Paint helpers ───────────────────────────────────────────────────────
    private fun paint(color: Int, style: Paint.Style): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = style
        }
    }

    private fun strokePaint(color: Int, width: Float): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
        }
    }

    private fun textPaint(color: Int, size: Float): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
}
