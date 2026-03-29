package com.swapnil.smart.aaos.ui

import com.swapnil.smart.aaos.media.Song

object NavigationCallback {
    var onPlaySong: ((Song) -> Unit)? = null
    var onPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onOpenDashboard: (() -> Unit)? = null
}