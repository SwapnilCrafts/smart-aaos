package com.swapnil.smart.aaos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.swapnil.smart.aaos.media.SmartMediaService

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, SmartMediaService::class.java)
        startForegroundService(intent)

        finish()
    }
}