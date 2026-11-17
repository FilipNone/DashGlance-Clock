package com.filipnone.dashglanceclock.clock

import androidx.annotation.ColorInt

data class ClockPalette(
    @ColorInt val background: Int,
    @ColorInt val dial: Int,
    @ColorInt val ticks: Int,
    @ColorInt val hands: Int,
    @ColorInt val accent: Int,
    @ColorInt val primaryText: Int,
    @ColorInt val secondaryText: Int,
) {
    companion object {
        val Dark = ClockPalette(
            background = 0xFF000000.toInt(),
            dial = 0xFF141414.toInt(),
            ticks = 0xFF8A8A8A.toInt(),
            hands = 0xFFF5F5F5.toInt(),
            accent = 0xFFFF8A3D.toInt(),
            primaryText = 0xFFF5F5F5.toInt(),
            secondaryText = 0xFFA6A6A6.toInt(),
        )

        val Light = ClockPalette(
            background = 0xFFF2F2F2.toInt(),
            dial = 0xFFFFFFFF.toInt(),
            ticks = 0xFF6B6B6B.toInt(),
            hands = 0xFF1A1A1A.toInt(),
            accent = 0xFFD9480F.toInt(),
            primaryText = 0xFF1A1A1A.toInt(),
            secondaryText = 0xFF5C5C5C.toInt(),
        )

        fun forDarkMode(isDarkMode: Boolean): ClockPalette = if (isDarkMode) Dark else Light
    }
}
