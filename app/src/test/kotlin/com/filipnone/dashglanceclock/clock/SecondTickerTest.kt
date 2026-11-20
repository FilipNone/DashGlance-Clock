package com.filipnone.dashglanceclock.clock

import android.os.Looper
import android.os.SystemClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun ticksEveryFrame_whileAnimating() {
        ticker.motion = Motion.CONTINUOUS
        ticker.start()
        looper.idleFor(Duration.ofMillis(SecondTicker.ANIMATION_FRAME_MILLIS * 10))

        assertEquals(11, ticks)
        ticker.stop()
    }

    @Test
    fun flipDesigns_tickEveryFrameOnlyWhileTheCardsTurn() {
        // Start two seconds before a minute changes.
        val toMinute = 60_000L - SystemClock.uptimeMillis() % 60_000L
        looper.idleFor(Duration.ofMillis(toMinute + 58_000L))
        ticker.motion = Motion.EACH_MINUTE
        ticker.start()

        looper.idleFor(Duration.ofMillis(1_500))
        val beforeMinute = ticks
        looper.idleFor(Duration.ofMillis(1_000))
        val whileTurning = ticks - beforeMinute
        looper.idleFor(Duration.ofMillis(1_000))
        val afterwards = ticks
        looper.idleFor(Duration.ofMillis(1_000))

        assertTrue("$whileTurning ticks while turning", whileTurning >= 10)
        assertEquals(1, ticks - afterwards)
        ticker.stop()
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
