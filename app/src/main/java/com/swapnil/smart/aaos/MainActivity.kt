package com.swapnil.smart.aaos

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestAudioPermission()
    }
    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 100)
        } else {
            MusicData.init(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            // init regardless — granted loads local, denied loads fallback
            MusicData.init(this)
        }
    }
}