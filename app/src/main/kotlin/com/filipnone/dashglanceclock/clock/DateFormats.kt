package com.filipnone.dashglanceclock.clock

import android.text.format.DateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.EnumMap
import java.util.Locale

internal class DateFormats(private val locale: Locale) {

    private val formatters = EnumMap<DateStyle, DateTimeFormatter>(DateStyle::class.java)

    fun format(dateTime: LocalDateTime, style: DateStyle): String =
        dateTime.format(formatters.getOrPut(style) { create(style) })

    private fun create(style: DateStyle): DateTimeFormatter = when (style) {
        DateStyle.SHORT -> DateTimeFormatter.ofPattern(SHORT_PATTERN, locale)
        DateStyle.MEDIUM -> bestPattern("EEEMMMd")
        DateStyle.LONG -> bestPattern("EEEEMMMMd")
        DateStyle.FULL -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
        // Camcorders printed English month names, and the pixel font only has Latin capitals.
        DateStyle.STAMP -> DateTimeFormatter.ofPattern(STAMP_PATTERN, Locale.ENGLISH)
    }

    private fun bestPattern(skeleton: String): DateTimeFormatter = try {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    } catch (_: IllegalArgumentException) {
        DateTimeFormatter.ofPattern(SHORT_PATTERN, locale)
    }

    private companion object {
        const val SHORT_PATTERN = "EEE d MMM"
        const val STAMP_PATTERN = "MMM. d yyyy"
    }
}
