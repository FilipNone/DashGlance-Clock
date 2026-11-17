package com.filipnone.dashglanceclock.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.ClockPalette
import com.filipnone.dashglanceclock.clock.ClockRenderer
import com.filipnone.dashglanceclock.clock.SecondTicker
import java.time.LocalDateTime

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val renderer = ClockRenderer()
    private val ticker = SecondTicker { invalidate() }
    private val clockBounds = RectF()

    var palette: ClockPalette
        get() = renderer.palette
        set(value) {
            renderer.palette = value
            invalidate()
        }

    var face: ClockFace = ClockFace.ANALOG
        set(value) {
            field = value
            invalidate()
        }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) ticker.start() else ticker.stop()
    }

    override fun onDetachedFromWindow() {
        ticker.stop()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        clockBounds.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat(),
        )
        renderer.draw(canvas, clockBounds, LocalDateTime.now(), face, DateFormat.is24HourFormat(context))
    }
}
