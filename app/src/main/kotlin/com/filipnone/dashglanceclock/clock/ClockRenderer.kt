package com.filipnone.dashglanceclock.clock

import android.graphics.Canvas
import android.graphics.RectF
import java.time.LocalDateTime
import java.util.Locale

class ClockRenderer(locale: Locale = Locale.getDefault()) {

    var palette: ClockPalette = ClockPalette.Dark

    private val analog = AnalogFace(locale)
    private val digital = DigitalFace(locale)

    fun drawBackground(canvas: Canvas) {
        canvas.drawColor(palette.background)
    }

    fun draw(
        canvas: Canvas,
        bounds: RectF,
        now: LocalDateTime,
        design: ClockDesign,
        use24Hour: Boolean,
    ) {
        if (bounds.width() <= 0f || bounds.height() <= 0f) return
        when (design) {
            is AnalogDesign -> analog.draw(canvas, bounds, now, design, palette)
            is DigitalDesign -> digital.draw(canvas, bounds, now, design, palette, use24Hour)
        }
    }
}
