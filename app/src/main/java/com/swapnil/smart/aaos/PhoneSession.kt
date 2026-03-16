package com.swapnil.smart.aaos

import android.content.Intent
import androidx.car.app.Session
import com.swapnil.smart.aaos.phone.screens.PhoneHomeScreen

class PhoneSession : Session() {

    override fun onCreateScreen(intent: Intent) =
        PhoneHomeScreen(carContext)
}