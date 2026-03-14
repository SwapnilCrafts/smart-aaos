package com.swapnil.smart.aaos

import android.content.Intent
import androidx.car.app.Session
import com.swapnil.smart.aaos.screens.HomeScreen

class SmartSession : Session() {

    override fun onCreateScreen(intent: Intent) =
        HomeScreen(carContext)
}