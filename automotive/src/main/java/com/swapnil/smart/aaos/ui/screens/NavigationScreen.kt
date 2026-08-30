package com.swapnil.smart.aaos.ui.screens

import android.text.SpannableString
import android.text.Spanned
import android.util.Log
import androidx.car.app.AppManager
import androidx.car.app.CarContext
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

    private val destinations = listOf(
        NavDestination(
            "Downtown SF", "Market St & 4th St, San Francisco",
            37.7749, -122.4194, 6.2
        ),
        NavDestination(
            "Charging Station", "SoMa DC Fast Chargers, 5th St",
            37.7810, -122.4010, 3.1
        ),
        NavDestination(
            "SFO Airport", "International Terminal, San Mateo",
            37.6213, -122.3790, 21.8
        ),
        NavDestination(
            "Golden Gate Bridge", "Golden Gate Bridge, San Francisco",
            37.8199, -122.4783, 14.5
        )
    )

    private var activeDestination: NavDestination? = null
    private val surfaceRenderer = MapSurfaceRenderer { activeDestination }

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceRenderer)
        if (startActive) {
            activeDestination = destinations.first()
        }
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
            val title = SpannableString(dest.name)
            title.setSpan(
                DistanceSpan.create(
                    Distance.create(dest.distanceKm, Distance.UNIT_KILOMETERS)
                ),
                0,
                dest.name.length,
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

        val anchor = destinations.firstOrNull()?.let {
            Place.Builder(
                CarLocation.create(it.lat, it.lng)
            )
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
            .setOnClickListener {
                activeDestination = null
                invalidate()
            }
            .build()

        // MapTemplate requires a Pane OR ItemList; a compact single-row Pane
        // keeps chrome minimal so the drawn route/map stays dominant.
        val etaMinutes = ((destination.distanceKm / 55.0) * 60.0).toLong()
        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("${destination.distanceKm} km · ${etaMinutes} min")
                    .addText("To ${destination.name}")
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
}