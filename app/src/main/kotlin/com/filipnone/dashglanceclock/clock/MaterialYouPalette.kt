package com.filipnone.dashglanceclock.clock

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi

// Material 3 tones run from 0 (black) to 100 (white). Android numbers its wallpaper colors the
// other way round, so system_accent1_800 is tone 20 of the primary color. Phones older than
// Android 12 have no wallpaper colors and get Material's baseline purple.
internal object MaterialYouPalette {

    fun resolve(context: Context, isDarkMode: Boolean): ClockPalette {
        val tones = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) wallpaperTones(context) else BaselineTones
        return if (isDarkMode) {
            ClockPalette(
                background = tones.neutral10,
                dial = tones.secondary20,
                ticks = tones.secondary60,
                hourHand = tones.primary80,
                hands = tones.tertiary80,
                accent = tones.tertiary90,
                primaryText = tones.neutral90,
                secondaryText = tones.neutralVariant80,
            )
        } else {
            ClockPalette(
                background = tones.neutral95,
                dial = tones.primary90,
                ticks = tones.secondary60,
                hourHand = tones.primary40,
                hands = tones.tertiary40,
                accent = tones.primary20,
                primaryText = tones.neutral10,
                secondaryText = tones.neutralVariant30,
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun wallpaperTones(context: Context) = Tones(
        primary20 = context.getColor(android.R.color.system_accent1_800),
        primary40 = context.getColor(android.R.color.system_accent1_600),
        primary80 = context.getColor(android.R.color.system_accent1_200),
        primary90 = context.getColor(android.R.color.system_accent1_100),
        secondary20 = context.getColor(android.R.color.system_accent2_800),
        secondary60 = context.getColor(android.R.color.system_accent2_400),
        tertiary40 = context.getColor(android.R.color.system_accent3_600),
        tertiary80 = context.getColor(android.R.color.system_accent3_200),
        tertiary90 = context.getColor(android.R.color.system_accent3_100),
        neutral10 = context.getColor(android.R.color.system_neutral1_900),
        neutral90 = context.getColor(android.R.color.system_neutral1_100),
        neutral95 = context.getColor(android.R.color.system_neutral1_50),
        neutralVariant30 = context.getColor(android.R.color.system_neutral2_700),
        neutralVariant80 = context.getColor(android.R.color.system_neutral2_200),
    )

    private val BaselineTones = Tones(
        primary20 = rgb(0x381E72),
        primary40 = rgb(0x6750A4),
        primary80 = rgb(0xD0BCFF),
        primary90 = rgb(0xEADDFF),
        secondary20 = rgb(0x332D41),
        secondary60 = rgb(0x958DA5),
        tertiary40 = rgb(0x7D5260),
        tertiary80 = rgb(0xEFB8C8),
        tertiary90 = rgb(0xFFD8E4),
        neutral10 = rgb(0x1C1B1F),
        neutral90 = rgb(0xE6E1E5),
        neutral95 = rgb(0xF4EFF4),
        neutralVariant30 = rgb(0x49454F),
        neutralVariant80 = rgb(0xCAC4D0),
    )

    private class Tones(
        @ColorInt val primary20: Int,
        @ColorInt val primary40: Int,
        @ColorInt val primary80: Int,
        @ColorInt val primary90: Int,
        @ColorInt val secondary20: Int,
        @ColorInt val secondary60: Int,
        @ColorInt val tertiary40: Int,
        @ColorInt val tertiary80: Int,
        @ColorInt val tertiary90: Int,
        @ColorInt val neutral10: Int,
        @ColorInt val neutral90: Int,
        @ColorInt val neutral95: Int,
        @ColorInt val neutralVariant30: Int,
        @ColorInt val neutralVariant80: Int,
    )
}
