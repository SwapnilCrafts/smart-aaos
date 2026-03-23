package com.swapnil.smart.aaos.car

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import androidx.car.app.Session
import com.swapnil.smart.aaos.media.SmartMusicService
import com.swapnil.smart.aaos.ui.screens.HomeScreen

class SmartSession : Session() {

    // ✅ Declare browser at class level — fixes reference issue
    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null

    override fun onCreateScreen(intent: Intent) =
        HomeScreen(carContext).also {
            handleVoiceIntent(intent)
        }

    override fun onNewIntent(intent: Intent) {
        handleVoiceIntent(intent)
    }

    private fun handleVoiceIntent(intent: Intent) {
        if (intent.action != MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) return

        val query = intent.getStringExtra(SearchManager.QUERY) ?: return

        // ✅ browser declared at class level — no reference issue
        mediaBrowser = MediaBrowserCompat(
            carContext,
            ComponentName(carContext, SmartMusicService::class.java),
            object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    // ✅ mediaBrowser not null here — safe to use
                    mediaController = MediaControllerCompat(
                        carContext,
                        mediaBrowser!!.sessionToken
                    )
                    mediaController?.transportControls
                        ?.playFromSearch(query, null)
                }
            },
            null
        )
        mediaBrowser?.connect()
    }
}