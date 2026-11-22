package com.filipnone.dashglanceclock.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import com.filipnone.dashglanceclock.clock.ClockDesign
import com.filipnone.dashglanceclock.clock.ClockDesigns
import com.filipnone.dashglanceclock.clock.ClockRenderer
import com.filipnone.dashglanceclock.clock.SecondTicker
import java.time.LocalDateTime

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), PartlyCovered {

    private val renderer = ClockRenderer()
    private val ticker = SecondTicker { invalidate() }
    private val clockBounds = RectF()
    private val cardBounds = RectF()
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var design: ClockDesign = ClockDesigns.DashGlanceAnalog
        set(value) {
            field = value
            ticker.motion = value.motion
            updatePalette()
            invalidate()
        }

    var cornerRadius: Float = DEFAULT_CORNER_RADIUS_DP * resources.displayMetrics.density
        set(value) {
            field = value
            invalidate()
        }

    // The card and clock are drawn above the covered part.
    override var coveredBottom: Int = 0
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    // Android Auto's media card is always dark, so DashGlance Card shows its designs in their
    // dark colors even when the phone is in light mode.
    var alwaysDark: Boolean = false
        set(value) {
            field = value
            updatePalette()
            invalidate()
        }

    init {
        updatePalette()
    }

    private fun updatePalette() {
        val isDarkMode = alwaysDark || resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        renderer.palette = design.palette(context, isDarkMode)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updatePalette()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) ticker.start() else ticker.stop()
    }

    override fun onDetachedFromWindow() {
        ticker.stop()
        super.onDetachedFromWindow()
    }

    // The rounded card in the design's background color shows roughly how the car screen will
    // look. With the DashGlance design it matches the app background and can't be seen.
    override fun onDraw(canvas: Canvas) {
        val shownHeight = (height - coveredBottom).coerceAtLeast(0)
        cardBounds.set(0f, 0f, width.toFloat(), shownHeight.toFloat())
        cardPaint.color = renderer.palette.background
        canvas.drawRoundRect(cardBounds, cornerRadius, cornerRadius, cardPaint)

        clockBounds.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (shownHeight - paddingBottom).toFloat(),
        )
        renderer.draw(canvas, clockBounds, LocalDateTime.now(), design, DateFormat.is24HourFormat(context))
    }

    private companion object {
        const val DEFAULT_CORNER_RADIUS_DP = 24f
    }
}
