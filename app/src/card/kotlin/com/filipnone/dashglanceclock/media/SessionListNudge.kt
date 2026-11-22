package com.filipnone.dashglanceclock.media

import android.content.Context
import android.media.session.MediaSession
import android.os.Handler
import android.os.Looper

// Android Auto's media card goes to the first playing session in the list it last got from
// MediaSessionManager. Android only sends a new list when a session is created, released or
// switched on or off, not when one starts playing. DashGlance starts playing just after the
// music app, so Android has already moved it to the top, but Android Auto's copy can still
// have the music app first. Opening and releasing an empty session makes Android send the
// current list. docs/android-auto.md has the test results.
class SessionListNudge(
    context: Context,
    // Opens the spare session and returns what releases it. Tests swap in a counter.
    private val openSpare: () -> () -> Unit = {
        val spare = MediaSession(context, SPARE_TAG)
        spare.isActive = true
        spare::release
    },
) {

    private val handler = Handler(Looper.getMainLooper())
    private var releaseSpare: (() -> Unit)? = null

    private val open = Runnable {
        closeSpare()
        releaseSpare = openSpare()
        handler.postDelayed(close, SPARE_LIFETIME_MS)
    }

    private val close = Runnable { closeSpare() }

    // Several calls close together send one list. The delay lets the music app and DashGlance
    // settle on their new playback states first.
    fun nudge() {
        handler.removeCallbacks(open)
        handler.postDelayed(open, DELAY_MS)
    }

    fun release() {
        handler.removeCallbacks(open)
        handler.removeCallbacks(close)
        closeSpare()
    }

    private fun closeSpare() {
        releaseSpare?.invoke()
        releaseSpare = null
    }

    internal companion object {
        const val SPARE_TAG = "DashGlanceNudge"
        const val DELAY_MS = 500L
        const val SPARE_LIFETIME_MS = 300L
    }
}
