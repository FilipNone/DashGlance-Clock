package com.filipnone.dashglanceclock.clock

import android.os.Looper
import android.os.SystemClock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class SecondTickerTest {

    private val looper = shadowOf(Looper.getMainLooper())
    private var ticks = 0
    private val ticker = SecondTicker(clock = SystemClock::uptimeMillis) { ticks++ }

    @Test
    fun ticksImmediatelyThenOncePerSecondUntilStopped() {
        ticker.start()
        assertEquals(1, ticks)

        looper.idleFor(Duration.ofSeconds(3))
        assertEquals(4, ticks)

        ticker.stop()
        looper.idleFor(Duration.ofSeconds(3))
        assertEquals(4, ticks)
    }

    @Test
    fun startingTwiceDoesNotDoubleTheTickRate() {
        ticker.start()
        ticker.start()
        looper.idleFor(Duration.ofSeconds(2))

        assertEquals(3, ticks)
        ticker.stop()
    }
}
