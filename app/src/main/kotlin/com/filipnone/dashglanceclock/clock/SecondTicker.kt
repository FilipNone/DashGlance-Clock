package com.filipnone.dashglanceclock.clock

import android.os.Handler
import android.os.Looper

// Ticks once a second, a few milliseconds after the second changes. Moving designs get a tick
// every frame instead: all the time, or only while a flip clock's cards turn.
class SecondTicker(
    private val clock: () -> Long = System::currentTimeMillis,
    private val onTick: () -> Unit,
) {

    var motion: Motion = Motion.NONE

    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            onTick()
            val now = clock()
            val animating = when (motion) {
                Motion.NONE -> false
                Motion.CONTINUOUS -> true
                Motion.EACH_MINUTE -> now % MILLIS_PER_MINUTE < Motion.FLIP_MILLIS
            }
            val delay = if (animating) {
                ANIMATION_FRAME_MILLIS
            } else {
                ClockMath.millisUntilNextSecond(now) + TICK_SLACK_MS
            }
            handler.postDelayed(this, delay)
        }
    }

    fun start() {
        if (running) return
        running = true
        tick.run()
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
    }

    companion object {
        // 25 frames a second. The car surface is drawn in software, so this is about as fast as
        // a glow and a full-screen grid can be redrawn there without dropping frames.
        const val ANIMATION_FRAME_MILLIS = 40L

        // Schedules each tick a few milliseconds after the second changes. A tick that fired
        // even 1 ms early would draw the previous second again.
        private const val TICK_SLACK_MS = 5L
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
