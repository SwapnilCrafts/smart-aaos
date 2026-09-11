package com.swapnil.smart.aaos.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.Pane
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapTemplate
import com.swapnil.smart.aaos.utils.CarLocationProvider
import java.util.Locale

data class NavDestination(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double
)

class NavigationScreen(
    carContext: CarContext,
    startActive: Boolean = false
) : Screen(carContext) {

    private val destinations = defaultDestinations()

    private var activeDestination: NavDestination? = null
    private val surfaceRenderer = MapSurfaceRenderer { activeDestination }

    /** Begin active navigation to [dest] immediately (used from the Home tab). */
    fun startNavigation(dest: NavDestination) {
        activeDestination = dest
        surfaceRenderer.redraw()
        invalidate()
    }

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceRenderer)
        if (startActive) {
            activeDestination = destinations.first()
        }

        // Anchor the map on the real vehicle position when we can get it.
        if (CarLocationProvider.hasPermission(carContext)) {
            CarLocationProvider.start(carContext) { invalidate() }
        } else {
            try {
                carContext.requestPermissions(listOf(CarLocationProvider.PERMISSION)) { approved, _ ->
                    if (approved.contains(CarLocationProvider.PERMISSION)) {
                        CarLocationProvider.start(carContext) { invalidate() }
                        invalidate()
                    }
                }
            } catch (e: Exception) {
                Log.d("NavScreen", "Location permission request unavailable: ${e.message}")
            }
        }

        // This screen holds two states (picker / navigating), so a single back
        // press should unwind one step rather than dropping the whole screen.
        // Action.BACK is routed through this dispatcher by the host, so the
        // header button, any hardware back key and the host's own gesture all
        // land here. Bound to the Screen lifecycle, so it stops intercepting
        // once another screen is on top.
        carContext.onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (activeDestination != null) {
                        stopNavigation()
                    } else {
                        screenManager.pop()
                    }
                }
            }
        )
    }

    /**
     * Hands turn-by-turn over to the head unit's navigation app, keeping this
     * app as the place-discovery surface - the pattern EV-charging / parking /
     * restaurant car apps use rather than shipping their own map engine.
     *
     * Two different mechanisms, because the automotive and projected cases
     * differ:
     *  - Native AAOS: this is an ordinary app on the head unit, so a standard
     *    ACTION_VIEW `geo:` intent is what the installed maps app handles.
     *    CarContext.startCarApp() does NOT work here - it targets the
     *    projected templates host and fails with "Remote startCarApp call
     *    failed".
     *  - Android Auto (the phone `app` module, or a projected host): there is
     *    no activity to start, so ACTION_NAVIGATE via startCarApp is correct.
     *
     * Try the native path first, fall back to the projected one, and tell the
     * user plainly when the car simply has no navigation app. Note the bare
     * AAOS emulator images ship only `com.android.car.mapsplaceholder` and so
     * resolve neither - use an AVD with Google Play to exercise this.
     */
    private fun handOffToNavApp(destination: NavDestination) {
        val label = Uri.encode(destination.name)
        val geo = Uri.parse(
            "geo:${destination.lat},${destination.lng}" +
                "?q=${destination.lat},${destination.lng}($label)"
        )
        Log.d("NavScreen", "hand-off -> $geo")

        val viewIntent = Intent(Intent.ACTION_VIEW, geo)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Resolve first so a missing maps app is a message, not an exception.
        val resolved = carContext.packageManager
            .resolveActivity(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolved != null) {
            try {
                carContext.startActivity(viewIntent)
                Log.d("NavScreen", "handed off to ${resolved.activityInfo?.packageName}")
                return
            } catch (e: Exception) {
                Log.w("NavScreen", "startActivity(geo:) failed: ${e.message}")
            }
        }

        // Projected host fallback.
        try {
            carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, geo))
            Log.d("NavScreen", "handed off via ACTION_NAVIGATE")
            return
        } catch (e: Exception) {
            Log.w("NavScreen", "ACTION_NAVIGATE failed: ${e.message}")
        }

        CarToast.makeText(
            carContext,
            "No navigation app installed on this car",
            CarToast.LENGTH_LONG
        ).show()
    }

    /** Leaves active guidance and returns to the destination picker. */
    fun stopNavigation() {
        activeDestination = null
        surfaceRenderer.redraw()
        invalidate()
    }

    override fun onGetTemplate(): Template {
        val destination = activeDestination
        Log.d(
            "NavScreen",
            "onGetTemplate -> ${if (destination == null) "PICKER" else "NAV: ${destination.name}"}"
        )
        return if (destination == null) buildDestinationPicker() else buildNavigation(destination)
    }

    // ── State 1: pick a destination on a map with place markers ─────────────
    private fun buildDestinationPicker(): Template {
        val navSpeedKmh = 55.0

        val listBuilder = ItemList.Builder()
        destinations.forEach { dest ->
            // DistanceSpan SUBSTITUTES its distance for the spanned range, so
            // the span must cover a placeholder - covering the whole name would
            // erase it. Two leading chars become "<distance> · <name>".
            val title = SpannableString("  ${'\u00b7'} ${dest.name}")
            title.setSpan(
                DistanceSpan.create(
                    Distance.create(distanceKm(dest), Distance.UNIT_KILOMETERS)
                ),
                0,
                1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .addText(dest.address)
                    .setOnClickListener {
                        activeDestination = dest
                        surfaceRenderer.redraw()
                        invalidate()
                    }
                    .build()
            )
        }

        // The host centres its real map on this anchor, so a live fix puts the
        // map wherever the car actually is; otherwise fall back to the first
        // destination so the map still shows the right region.
        val fix = CarLocationProvider.current
        val anchorLocation = if (fix != null) {
            CarLocation.create(fix.latitude, fix.longitude)
        } else {
            destinations.firstOrNull()?.let { CarLocation.create(it.lat, it.lng) }
        }
        val anchor = anchorLocation?.let {
            Place.Builder(it)
                .setMarker(
                    PlaceMarker.Builder()
                        .setLabel("You")
                        .setColor(CarColor.GREEN)
                        .build()
                )
                .build()
        }

        val pickerBuilder = PlaceListMapTemplate.Builder()
            .setTitle("Choose Destination")
            .setHeaderAction(Action.BACK)
            .setItemList(listBuilder.build())
            .setLoading(false)

        if (anchor != null) {
            pickerBuilder.setAnchor(anchor)
        }

        return pickerBuilder.build()
    }

    // ── State 2: in-navigation, full-screen drawn route map ─────────────────
    // Uses MapTemplate (rather than MapWithContentTemplate) because
    // MapWithContentTemplate always overlays a content panel on top of the map,
    // hiding our drawn route. MapTemplate shows the map full-screen — the
    // header/pane/item list are optional, so we provide only a compact header
    // and the action strip, leaving the entire surface as the map+route.
    private fun buildNavigation(destination: NavDestination): Template {
        val changeDestinationAction = Action.Builder()
            .setTitle("Destinations")
            .setOnClickListener { stopNavigation() }
            .build()

        // MapTemplate requires a Pane OR ItemList; a compact single-row Pane
        // keeps chrome minimal so the drawn route/map stays dominant.
        val distance = distanceKm(destination)
        val etaMinutes = ((distance / 55.0) * 60.0).toLong()
        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle(String.format(Locale.US, "%.1f km · %d min", distance, etaMinutes))
                    .addText("To ${destination.name}")
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Start in Maps")
                    .setOnClickListener { handOffToNavApp(destination) }
                    .build()
            )
            .build()

        val mapController = MapController.Builder()
            .setPanModeListener { panActive ->
                Log.d("NavScreen", "pan mode active=$panActive")
                surfaceRenderer.redraw()
            }
            .build()

        return MapTemplate.Builder()
            .setMapController(mapController)
            .setHeader(
                Header.Builder()
                    .setTitle("Navigation")
                    // Without a start header action the host draws no back
                    // affordance at all, which strands the user on this screen.
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .setPane(pane)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(changeDestinationAction)
                    .build()
            )
            .build()
    }

    companion object {
        fun defaultDestinations(): List<NavDestination> = listOf(
            NavDestination(
                "Gateway of India", "Apollo Bandar, Colaba, Mumbai",
                18.9220, 72.8347, 18.4
            ),
            NavDestination(
                "BKC Charging Hub", "Bandra Kurla Complex, Mumbai",
                19.0662, 72.8685, 1.6
            ),
            NavDestination(
                "CSMI Airport T2", "Terminal 2, Andheri East, Mumbai",
                19.0896, 72.8656, 2.3
            ),
            NavDestination(
                "Bandra-Worli Sea Link", "Sea Link, Mumbai",
                19.0330, 72.8200, 8.1
            )
        )

        /**
         * Distance to [dest] from the live fix, falling back to the static
         * value in [NavDestination] when no fix is available yet.
         */
        fun distanceKm(dest: NavDestination): Double =
            CarLocationProvider.distanceKmTo(dest.lat, dest.lng) ?: dest.distanceKm
    }
}