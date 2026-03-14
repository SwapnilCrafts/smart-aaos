package com.swapnil.smart.aaos

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

class HomeScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return PaneTemplate.Builder(
            Pane.Builder().addRow(
                Row.Builder().setTitle("Smart Drive AAOS").build()
            ).build()
        )
            .setTitle("Home")
            .build()
    }
}
