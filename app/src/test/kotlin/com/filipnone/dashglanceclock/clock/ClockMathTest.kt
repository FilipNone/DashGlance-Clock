package com.filipnone.dashglanceclock.clock

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class ClockMathTest {

    @Test
    fun hourHand_pointsAtTheHourAndAdvancesWithMinutesAndSeconds() {
        assertEquals(0f, ClockMath.hourHandDegrees(LocalTime.of(0, 0)), DELTA)
        assertEquals(0f, ClockMath.hourHandDegrees(LocalTime.of(12, 0)), DELTA)
        assertEquals(90f, ClockMath.hourHandDegrees(LocalTime.of(15, 0)), DELTA)
        assertEquals(315f, ClockMath.hourHandDegrees(LocalTime.of(10, 30)), DELTA)
        assertEquals(195.25f, ClockMath.hourHandDegrees(LocalTime.of(6, 30, 30)), DELTA)
    }

    @Test
    fun minuteHand_advancesSixDegreesPerMinuteAndSmoothlyWithSeconds() {
        assertEquals(0f, ClockMath.minuteHandDegrees(LocalTime.of(9, 0)), DELTA)
        assertEquals(90f, ClockMath.minuteHandDegrees(LocalTime.of(9, 15)), DELTA)
        assertEquals(93f, ClockMath.minuteHandDegrees(LocalTime.of(9, 15, 30)), DELTA)
        assertEquals(359.9f, ClockMath.minuteHandDegrees(LocalTime.of(9, 59, 59)), DELTA)
    }

    @Test
    fun secondHand_advancesSixDegreesPerSecond() {
        assertEquals(0f, ClockMath.secondHandDegrees(LocalTime.of(9, 0, 0)), DELTA)
        assertEquals(270f, ClockMath.secondHandDegrees(LocalTime.of(9, 0, 45)), DELTA)
    }

    @Test
    fun millisUntilNextSecond_landsOnTheNextWholeSecond() {
        assertEquals(1_000L, ClockMath.millisUntilNextSecond(5_000L))
        assertEquals(750L, ClockMath.millisUntilNextSecond(5_250L))
        assertEquals(1L, ClockMath.millisUntilNextSecond(5_999L))
        assertEquals(250L, ClockMath.millisUntilNextSecond(-250L))
    }

    @Test
    fun clockFace_togglesAndParsesSafely() {
        assertEquals(ClockFace.DIGITAL, ClockFace.ANALOG.toggled())
        assertEquals(ClockFace.ANALOG, ClockFace.DIGITAL.toggled())
        assertEquals(ClockFace.DIGITAL, ClockFace.fromName("DIGITAL"))
        assertEquals(ClockFace.ANALOG, ClockFace.fromName(null))
        assertEquals(ClockFace.ANALOG, ClockFace.fromName("SUNDIAL"))
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
