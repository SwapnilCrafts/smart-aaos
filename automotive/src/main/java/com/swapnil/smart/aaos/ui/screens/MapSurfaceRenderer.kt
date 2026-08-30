package com.swapnil.smart.aaos.ui.screens

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer

/**
 * Registers as a [androidx.car.app.SurfaceCallback] so the app is handed a
 * [SurfaceContainer] whenever a map-based template (e.g. `MapWithContentTemplate`)
 * is on screen. Each time the surface is (re)delivered we draw a stylized map:
 * a dark base, a projected route line from the origin to the chosen destination,
 * and the destination pin. Because the app controls every pixel this renders on
 * the AAOS emulator even though the host can't draw real Google polylines.
 */
class MapSurfaceRenderer(
    private val getDestination: () -> NavDestination?
) : SurfaceCallback {

    private var surfaceContainer: SurfaceContainer? = null

    // Interactive pan/zoom state applied as a transform to every draw, so the
    // user can drag (pan), fling, and pinch-zoom the map.
    private var offsetX = 0f
    private var offsetY = 0f
    private var scale = 1f

    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 168, 255)
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 168, 255)
        style = Paint.Style.STROKE
        strokeWidth = 22f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val destBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 80, 80)
        style = Paint.Style.FILL
    }

    private val destPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(200, 40, 40)
        style = Paint.Style.FILL
    }

    private val originBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 200, 120)
        style = Paint.Style.FILL
    }

    private val originPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 140, 80)
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(160, 178, 190)
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 38, 48)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        Log.d("NavScreen", "surface available ${surfaceContainer.width}x${surfaceContainer.height}")
        this.surfaceContainer = surfaceContainer
        draw()
    }

    override fun onVisibleAreaChanged(rect: Rect) = draw()
    override fun onStableAreaChanged(rect: Rect) = draw()

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        Log.d("NavScreen", "surface destroyed")
        this.surfaceContainer = null
    }

    /** Called after the destination changes so the route is re-stamped. */
    fun redraw() = draw()

    override fun onScroll(x: Float, y: Float) {
        offsetX += x
        offsetY += y
        Log.d("NavScreen", "onScroll dx=$x dy=$y -> offset=($offsetX,$offsetY)")
        draw()
    }

    override fun onFling(velocityXVelocity: Float, velocityYVelocity: Float) {
        // Simple drag-style response: shift a bit along the fling direction.
        offsetX += velocityXVelocity / 40f
        offsetY += velocityYVelocity / 40f
        Log.d("NavScreen", "onFling vx=$velocityXVelocity vy=$velocityYVelocity")
        draw()
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        val newScale = (scale * scaleFactor).coerceIn(0.5f, 4f)
        val factor = newScale / scale
        val oldScale = scale
        offsetX = focusX - (focusX - offsetX) * factor
        offsetY = focusY - (focusY - offsetY) * factor
        scale = newScale
        Log.d("NavScreen", "onScale factor=$scaleFactor -> scale=$scale")
        draw()
    }

    private fun draw() {
        val dest = getDestination() ?: return
        val holder = surfaceContainer ?: return
        val surface = holder.surface ?: return
        val width = holder.width
        val height = holder.height
        if (width <= 0 || height <= 0) return

        // Use the software canvas, not lockHardwareCanvas(). The host already
        // owns the GL/EGL connection for this Surface; requesting a hardware
        // canvas here triggers EGL_BAD_ALLOC / "already connected to another
        // API" and a native HWUI abort.
        val canvas = try {
            surface.lockCanvas(null)
        } catch (e: Exception) {
            Log.w("NavScreen", "lock canvas failed: ${e.message}")
            return
        }
        try {
            render(canvas, width, height, dest)
        } finally {
            try {
                surface.unlockCanvasAndPost(canvas)
            } catch (e: Exception) {
                Log.w("NavScreen", "unlock canvas failed: ${e.message}")
            }
        }
    }

    private fun render(c: Canvas, w: Int, h: Int, dest: NavDestination) {
        // Dark base (full-screen, untransformed so edges always stay covered)
        c.drawColor(Color.rgb(13, 17, 23))

        c.save()
        c.translate(offsetX, offsetY)
        c.scale(scale, scale)

        // Faint grid to suggest a city block map
        val stepX = w / 9
        val stepY = h / 7
        var y = 0
        while (y <= h) {
            c.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), gridPaint)
            y += stepY
        }
        var x = 0
        while (x <= w) {
            c.drawLine(x.toFloat(), 0f, x.toFloat(), h.toFloat(), gridPaint)
            x += stepX
        }

        val origin = ORIGIN
        val o = project(origin.lat, origin.lng, dest.lat, dest.lng, w, h)
        val d = project(dest.lat, dest.lng, dest.lat, dest.lng, w, h)

        // Rise along a smooth arc with a slight detour so it reads as a route.
        val points = routePoints(o, d, 24)
        c.drawPath(android.graphics.Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }, haloPaint)
        c.drawPath(
            android.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            },
            routePaint
        )

        // Origin pin (green)
        drawPin(c, o.x, o.y, originPinPaint, originBodyPaint, "You")

        // Destination pin (red) at screen-right, slightly lower for prominence
        val dp = project(dest.lat, dest.lng, dest.lat, dest.lng, w, h)
        drawPin(c, dp.x, dp.y, destPinPaint, destBodyPaint, dest.name)

        c.restore()
    }

    private fun drawPin(c: Canvas, x: Float, y: Float, shadow: Paint, body: Paint, label: String) {
        // Tear-drop pin: circle plus triangle tail
        c.drawCircle(x, y, 16f, shadow)
        val path = android.graphics.Path().apply {
            moveTo(x, y - 16f)
            lineTo(x - 12f, y + 10f)
            lineTo(x + 12f, y + 10f)
            close()
        }
        c.drawPath(path, shadow)
        c.drawCircle(x, y, 11f, body)
        c.drawText(shortLabel(label), x, y - 28f, labelPaint)
        c.drawText("destination", x, y - 4f, subLabelPaint)
    }

    private fun shortLabel(name: String): String =
        if (name.length > 10) name.take(7) + "…" else name

    private data class Pt(val x: Float, val y: Float)

    private fun routePoints(o: Pt, d: Pt, n: Int): List<Pt> {
        val pts = ArrayList<Pt>(n + 1)
        for (i in 0..n) {
            val t = i / n.toFloat()
            val x = o.x + (d.x - o.x) * t
            val y = o.y + (d.y - o.y) * t
            val bulge = 0.22f * Math.sin(Math.PI * t).toFloat()
            pts.add(Pt(x + bulge, y - Math.abs(bulge) * 1.4f))
        }
        return pts
    }

    // Project lat/lng into screen space: longitudes left->right, latitudes
    // top->bottom, fitted to the surface.
    private fun project(
        lat: Double, lng: Double,
        destLat: Double, destLng: Double,
        w: Int, h: Int
    ): Pt {
        val minLat = minOf(lat, destLat) - 0.06
        val maxLat = maxOf(lat, destLat) + 0.06
        val minLng = minOf(lng, destLng) - 0.06
        val maxLng = maxOf(lng, destLng) + 0.06
        val spanLng = maxOf(maxLng - minLng, 0.0001)
        val spanLat = maxOf(maxLat - minLat, 0.0001)

        val x = ((lng - minLng) / spanLng * w).toFloat()
        val y = ((1.0 - (lat - minLat) / spanLat) * h).toFloat()
        return Pt(x, y)
    }

    companion object {
        val ORIGIN = NavDestination("Origin", "Current location", 37.7793, -122.4193, 0.0)
    }
}
