package com.swapnil.smart.aaos.ui.screens

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.swapnil.smart.aaos.utils.CarLocationProvider

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

    // The host reports which part of the surface its own chrome (header, action
    // strip, pan controls) is NOT covering. Route/pin content is confined to
    // this, otherwise pins near a corner end up behind the action strip.
    private var visibleArea: Rect? = null
    private var stableArea: Rect? = null

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

    override fun onVisibleAreaChanged(rect: Rect) {
        visibleArea = Rect(rect)
        Log.d("NavScreen", "visible area $rect")
        draw()
    }

    override fun onStableAreaChanged(rect: Rect) {
        stableArea = Rect(rect)
        Log.d("NavScreen", "stable area $rect")
        draw()
    }

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

        val origin = origin()
        val area = contentBounds(w, h)
        val o = project(origin.lat, origin.lng, dest.lat, dest.lng, area)
        val d = project(dest.lat, dest.lng, origin.lat, origin.lng, area)

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
        drawPin(c, o.x, o.y, originPinPaint, originBodyPaint, "You", "you are here")

        // Destination pin (red)
        drawPin(c, d.x, d.y, destPinPaint, destBodyPaint, dest.name, "destination")

        c.restore()
    }

    /**
     * Region the route and pins may occupy: the host's stable area when known,
     * else the visible area, else the whole surface - then inset so a pin's
     * labels (drawn above the pin) stay on screen too.
     */
    private fun contentBounds(w: Int, h: Int): Rect {
        val surface = Rect(0, 0, w, h)
        val reported = stableArea?.takeIf { !it.isEmpty }
            ?: visibleArea?.takeIf { !it.isEmpty }
        val bounds = Rect(reported ?: surface)
        if (!bounds.intersect(surface)) bounds.set(surface)

        bounds.inset(LABEL_PAD_X, LABEL_PAD_Y)
        // A tiny stable area could invert the rect; fall back rather than draw
        // pins at negative coordinates.
        if (bounds.isEmpty) {
            bounds.set(surface)
            bounds.inset(LABEL_PAD_X, LABEL_PAD_Y)
        }
        return bounds
    }

    private fun drawPin(
        c: Canvas,
        x: Float,
        y: Float,
        shadow: Paint,
        body: Paint,
        label: String,
        subLabel: String
    ) {
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
        c.drawText(subLabel, x, y - 4f, subLabelPaint)
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
        area: Rect
    ): Pt {
        // Pad proportionally to the route, not by a fixed degree amount. A flat
        // 0.06 deg (~6.7 km) pad swamps any short route, collapsing both pins
        // onto the same pixel; scaling the pad keeps the route filling the
        // surface whether it is 1 km or 30 km long.
        val routeLat = kotlin.math.abs(destLat - lat)
        val routeLng = kotlin.math.abs(destLng - lng)
        val padLat = maxOf(routeLat * 0.30, 0.004)
        val padLng = maxOf(routeLng * 0.30, 0.004)

        val minLat = minOf(lat, destLat) - padLat
        val maxLat = maxOf(lat, destLat) + padLat
        val minLng = minOf(lng, destLng) - padLng
        val maxLng = maxOf(lng, destLng) + padLng
        val spanLng = maxOf(maxLng - minLng, 0.0001)
        val spanLat = maxOf(maxLat - minLat, 0.0001)

        val x = area.left + ((lng - minLng) / spanLng * area.width()).toFloat()
        val y = area.top + ((1.0 - (lat - minLat) / spanLat) * area.height()).toFloat()
        return Pt(x, y)
    }

    companion object {
        /** Headroom for the pin label stack drawn above each pin. */
        private const val LABEL_PAD_X = 90
        private const val LABEL_PAD_Y = 64

        /** Used only until a real fix arrives (Mumbai city centre). */
        private const val FALLBACK_LAT = 19.0760
        private const val FALLBACK_LNG = 72.8777

        /** Live vehicle position when available, else [FALLBACK_LAT]/[FALLBACK_LNG]. */
        fun origin(): NavDestination {
            val fix = CarLocationProvider.current
            return NavDestination(
                "You",
                "Current location",
                fix?.latitude ?: FALLBACK_LAT,
                fix?.longitude ?: FALLBACK_LNG,
                0.0
            )
        }
    }
}
