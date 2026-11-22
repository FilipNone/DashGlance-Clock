package com.filipnone.dashglanceclock.media

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class SessionListNudgeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var opened = 0
    private var released = 0
    private val nudge = SessionListNudge(context) {
        opened++
        { released++ }
    }

    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun aNudge_opensOneSpareSessionAfterTheDelayAndReleasesIt() {
        nudge.nudge()
        advance(SessionListNudge.DELAY_MS - 1)
        assertEquals(0, opened)

        advance(1)
        assertEquals(1, opened)
        assertEquals(0, released)

        advance(SessionListNudge.SPARE_LIFETIME_MS)
        assertEquals(1, released)
    }

    @Test
    fun nudgesInQuickSuccession_openOnlyOneSpare() {
        repeat(3) {
            nudge.nudge()
            advance(100)
        }
        advance(SessionListNudge.DELAY_MS + SessionListNudge.SPARE_LIFETIME_MS)

        assertEquals(1, opened)
        assertEquals(1, released)
    }

    @Test
    fun releaseBeforeTheDelay_opensNothing() {
        nudge.nudge()
        nudge.release()
        advance(SessionListNudge.DELAY_MS + SessionListNudge.SPARE_LIFETIME_MS)

        assertEquals(0, opened)
    }

    @Test
    fun releaseWhileTheSpareIsOpen_releasesItOnce() {
        nudge.nudge()
        advance(SessionListNudge.DELAY_MS)
        nudge.release()
        advance(SessionListNudge.SPARE_LIFETIME_MS)

        assertEquals(1, opened)
        assertEquals(1, released)
    }

    @Test
    fun theRealSpareSession_canBeOpenedAndReleased() {
        val real = SessionListNudge(context)
        real.nudge()
        advance(SessionListNudge.DELAY_MS + SessionListNudge.SPARE_LIFETIME_MS)
        real.release()
    }
}
