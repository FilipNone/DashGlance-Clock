package com.filipnone.dashglanceclock.media

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.filipnone.dashglanceclock.clock.ClockDesign
import com.filipnone.dashglanceclock.clock.ClockPalette
import com.filipnone.dashglanceclock.clock.ClockRenderer
import java.time.LocalDateTime
import java.util.Locale

// The square picture in the media card, with the clock where the card leaves it in view. The
// player sends it to Android Auto, and the phone screen draws it in its preview.
class CardArtwork(locale: Locale = Locale.getDefault()) {

    private val renderer = ClockRenderer(locale)
    private val fill = Paint()
    private val bounds = RectF()

    fun draw(
        canvas: Canvas,
        left: Float,
        top: Float,
        size: Float,
        now: LocalDateTime,
        design: ClockDesign,
        palette: ClockPalette,
        use24Hour: Boolean,
    ) {
        fill.color = palette.background
        canvas.drawRect(left, top, left + size, top + size, fill)
        bounds.set(
            left + size * CLOCK_LEFT,
            top + size * CLOCK_TOP,
            left + size * CLOCK_RIGHT,
            top + size * CLOCK_BOTTOM,
        )
        renderer.palette = palette
        renderer.draw(canvas, bounds, now, design, use24Hour)
    }

    companion object {
        // Fractions of the square. On a 1280 x 720 screen the card crops it to the middle 52%
        // of its width, Android Auto puts the app's icon along the top edge, and the card's
        // text covers the bottom 45%.
        const val CLOCK_LEFT = 0.24f
        const val CLOCK_RIGHT = 0.76f
        const val CLOCK_TOP = 0.08f
        const val CLOCK_BOTTOM = 0.54f
    }
}
