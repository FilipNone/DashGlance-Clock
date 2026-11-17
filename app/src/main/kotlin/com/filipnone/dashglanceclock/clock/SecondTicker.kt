package com.filipnone.dashglanceclock.clock

import android.os.Handler
import android.os.Looper

class SecondTicker(
    private val clock: () -> Long = System::currentTimeMillis,
    private val onTick: () -> Unit,
) {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            onTick()
            handler.postDelayed(
                this,
                ClockMath.millisUntilNextSecond(clock()) + TICK_SLACK_MS,
            )
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

    private companion object {
        // Schedules each tick a few milliseconds after the second changes. A tick that fired
        // even 1 ms early would draw the previous second again.
        const val TICK_SLACK_MS = 5L
    }
}
