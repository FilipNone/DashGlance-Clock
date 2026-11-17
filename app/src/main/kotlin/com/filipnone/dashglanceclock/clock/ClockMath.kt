package com.filipnone.dashglanceclock.clock

import java.time.LocalTime

// Hand angles are in degrees, clockwise from 12 o'clock.
object ClockMath {

    fun hourHandDegrees(time: LocalTime): Float =
        (time.hour % 12) * 30f + time.minute * 0.5f + time.second * (0.5f / 60f)

    fun minuteHandDegrees(time: LocalTime): Float =
        time.minute * 6f + time.second * 0.1f

    fun secondHandDegrees(time: LocalTime): Float =
        time.second * 6f

    fun millisUntilNextSecond(epochMillis: Long): Long =
        1_000L - Math.floorMod(epochMillis, 1_000L)
}
