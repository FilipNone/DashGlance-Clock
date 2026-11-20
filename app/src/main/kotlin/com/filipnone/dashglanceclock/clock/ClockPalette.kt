package com.filipnone.dashglanceclock.clock

import android.graphics.Color
import androidx.annotation.ColorInt

data class ClockPalette(
    @ColorInt val background: Int,
    @ColorInt val primaryText: Int,
    @ColorInt val secondaryText: Int,
    @ColorInt val accent: Int,
    // Designs that leave out the colors below get the defaults. Unlit, glow and panel are
    // only used by digital designs.
    @ColorInt val dial: Int = background,
    @ColorInt val ticks: Int = secondaryText,
    @ColorInt val hands: Int = primaryText,
    @ColorInt val hourHand: Int = hands,
    @ColorInt val dialEdge: Int = dial,
    @ColorInt val bezel: Int = dial,
    @ColorInt val bezelShade: Int = bezel,
    @ColorInt val unlit: Int = Color.TRANSPARENT,
    @ColorInt val glow: Int = Color.TRANSPARENT,
    @ColorInt val panel: Int = Color.TRANSPARENT,
) {

    @ColorInt
    fun color(ink: Ink): Int = when (ink) {
        Ink.BACKGROUND -> background
        Ink.DIAL -> dial
        Ink.BEZEL -> bezel
        Ink.TICKS -> ticks
        Ink.HANDS -> hands
        Ink.HOUR_HAND -> hourHand
        Ink.ACCENT -> accent
        Ink.PRIMARY -> primaryText
        Ink.SECONDARY -> secondaryText
    }

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

// Names a palette color, so a design can say which color each part uses.
enum class Ink { BACKGROUND, DIAL, BEZEL, TICKS, HANDS, HOUR_HAND, ACCENT, PRIMARY, SECONDARY }

@ColorInt
internal fun rgb(hex: Int, alpha: Int = 0xFF): Int = (alpha shl 24) or (hex and 0xFFFFFF)
