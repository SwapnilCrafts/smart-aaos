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

        // Speed labels (0/40/80/120/160/200) on a rotated canvas
        for (i in 0..10 step 2) {
            val a = Math.toRadians(135.0 + i * 27.0)
            c.save()
            c.rotate((135.0 + i * 27.0).toFloat(), cx, cy)
            c.drawText(
                (i * 20).toString(), cx + radius * 0.58f, cy + size * 0.014f,
                textPaint(Color.rgb(160, 175, 190), size * 0.032f)
            )
            c.restore()
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

        // RPM labels (2/4/6/8 in thousands) along the arc
        for (i in 1..4) {
            val a = Math.toRadians(180.0 + 180.0 * (i / 4.0))
            c.save()
            c.rotate((180.0 + 180.0 * (i / 4.0)).toFloat(), size / 2f, size / 2f)
            c.drawText(
                (i * 2).toString(), size / 2f + size * 0.52f, size / 2f + size * 0.012f,
                textPaint(Color.rgb(160, 175, 190), size * 0.032f)
            )
            c.restore()
        }

        val tp = textPaint(Color.WHITE, size * 0.12f)
        c.drawText(rpm.toInt().toString(), size / 2f, size * 0.5f, tp)
        c.drawText(
            "RPM", size / 2f, size * 0.5f + tp.textSize * 1.1f,
            textPaint(Color.rgb(140, 160, 180), size * 0.05f)
        )
        return bm
    }

    // ── Gear strip (P/R/N/D + engine state) ─────────────────────────────────
    fun drawGearStrip(gear: String, engineOn: Boolean, warn: Boolean, size: Int = 340): Bitmap {
        val h = (size * 0.30f).toInt().coerceAtLeast(72)
        val bm = Bitmap.createBitmap(size, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val gears = listOf("P", "R", "N", "D")
        val active = gear.uppercase().takeLast(1).let { if (it in gears) it else "P" }
        val slotW = size * 0.13f
        val gap = size * 0.035f
        val x0 = size * 0.04f
        gears.forEachIndexed { i, g ->
            val x = x0 + i * (slotW + gap)
            val selected = g == active
            if (selected) {
                c.drawRoundRect(
                    RectF(x, h * 0.16f, x + slotW, h * 0.64f), size * 0.025f, size * 0.025f,
                    paint(Color.rgb(0, 200, 120), Paint.Style.FILL)
                )
            } else {
                c.drawRoundRect(
                    RectF(x, h * 0.16f, x + slotW, h * 0.64f), size * 0.025f, size * 0.025f,
                    strokePaint(Color.rgb(90, 102, 114), size * 0.012f)
                )
            }
            c.drawText(
                g, x + slotW / 2f, h * 0.58f,
                textPaint(if (selected) Color.rgb(8, 10, 12) else Color.WHITE, size * 0.09f)
            )
        }
        val dotColor = when {
            warn -> Color.rgb(255, 180, 40)
            engineOn -> Color.rgb(0, 210, 120)
            else -> Color.rgb(120, 130, 140)
        }
        c.drawCircle(size * 0.82f, h * 0.28f, size * 0.022f, paint(dotColor, Paint.Style.FILL))
        c.drawText(
            if (engineOn) "ENGINE ON" else "ENGINE OFF",
            size * 0.5f, h * 0.86f,
            textPaint(Color.rgb(180, 190, 200), size * 0.042f)
        )
        return bm
    }

    // ── Fuel bar (0-100%) ───────────────────────────────────────────────────
    fun drawFuelBar(fuel: Float, size: Int = 340): Bitmap {
        val h = (size * 0.18f).toInt().coerceAtLeast(46)
        val bm = Bitmap.createBitmap(size, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val frac = (fuel / 100f).coerceIn(0f, 1f)

        val color = when {
            fuel < 15  -> Color.rgb(255, 70, 70)
            fuel < 30  -> Color.rgb(255, 180, 40)
            else       -> Color.rgb(0, 200, 120)
        }
        val inset = size * 0.02f
        drawSegmentedBar(c, inset, h * 0.30f, size - inset, h * 0.70f, frac, color, size * 0.03f)
        c.drawText(
            "Fuel  ${fuel.toInt()}%", size * 0.5f, h * 0.85f,
            textPaint(Color.WHITE, h * 0.55f)
        )
        c.drawCircle(size - size * 0.05f, h * 0.50f, size * 0.02f, paint(color, Paint.Style.FILL))
        return bm
    }

    // ── Battery bar (0-100%) ────────────────────────────────────────────────
    fun drawBatteryBar(level: Float, size: Int = 340): Bitmap {
        val h = (size * 0.18f).toInt().coerceAtLeast(46)
        val bm = Bitmap.createBitmap(size, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val frac = (level / 100f).coerceIn(0f, 1f)
        val color = if (level < 20) Color.rgb(255, 70, 70) else Color.rgb(80, 180, 255)
        val inset = size * 0.02f
        drawSegmentedBar(c, inset, h * 0.30f, size - inset, h * 0.70f, frac, color, size * 0.03f)
        c.drawText(
            "Battery  ${level.toInt()}%", size * 0.5f, h * 0.85f,
            textPaint(Color.WHITE, h * 0.55f)
        )
        c.drawCircle(size - size * 0.05f, h * 0.50f, size * 0.02f, paint(color, Paint.Style.FILL))
        return bm
    }

    // Segmented (tick-divided) rounded bar like an EV charge strip
    private fun drawSegmentedBar(
        c: Canvas, l: Float, t: Float, r: Float, b: Float,
        frac: Float, color: Int, radius: Float
    ) {
        val trackColor = Color.rgb(40, 48, 58)
        c.drawRoundRect(RectF(l, t, r, b), radius, radius, paint(trackColor, Paint.Style.FILL))
        val fillRight = l + (r - l) * frac
        if (fillRight > l + radius) {
            c.drawRoundRect(RectF(l, t, fillRight, b), radius, radius, paint(color, Paint.Style.FILL))
        }
        // segment dividers
        val div = strokePaint(Color.rgb(10, 12, 14), Math.max(3f, (r - l) * 0.006f))
        val n = 16
        for (i in 1 until n) {
            val x = l + (r - l) * (i / n.toFloat())
            c.drawLine(x, t + (b - t) * 0.18f, x, b - (b - t) * 0.18f, div)
        }
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

    /**
     * Centres a wide gauge bitmap inside a square canvas.
     *
     * The bar gauges are deliberately wide and short (340x61). A GridTemplate
     * image slot is square, so handing it a 5.6:1 bitmap wastes the slot and
     * distorts the result. Scaling to fit the width and padding vertically
     * keeps the bar's proportions while filling the square.
     */
    fun squared(source: Bitmap, size: Int = 360): Bitmap {
        if (source.width == source.height) return source
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val scale = size.toFloat() / source.width
        val h = source.height * scale
        val dst = android.graphics.RectF(
            0f, (size - h) / 2f, size.toFloat(), (size + h) / 2f
        )
        c.drawBitmap(source, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }
}
