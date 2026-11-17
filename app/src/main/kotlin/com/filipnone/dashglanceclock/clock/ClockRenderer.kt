package com.filipnone.dashglanceclock.clock

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.core.graphics.withRotation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class ClockRenderer(private val locale: Locale = Locale.getDefault()) {

    var palette: ClockPalette = ClockPalette.Dark

    private val analogDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    private val digitalDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    private val time24Formatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val time12Formatter = DateTimeFormatter.ofPattern("h:mm", locale)
    private val amPmFormatter = DateTimeFormatter.ofPattern("a", locale)
    private val secondsFormatter = DateTimeFormatter.ofPattern("ss", locale)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        fontFeatureSettings = "tnum"
    }
    private val textBounds = Rect()

    fun drawBackground(canvas: Canvas) {
        canvas.drawColor(palette.background)
    }

    fun draw(
        canvas: Canvas,
        bounds: RectF,
        now: LocalDateTime,
        face: ClockFace,
        use24Hour: Boolean,
    ) {
        if (bounds.width() <= 0f || bounds.height() <= 0f) return
        when (face) {
            ClockFace.ANALOG -> drawAnalog(canvas, bounds, now)
            ClockFace.DIGITAL -> drawDigital(canvas, bounds, now, use24Hour)
        }
    }

    private fun drawAnalog(canvas: Canvas, bounds: RectF, now: LocalDateTime) {
        val dial = Dial(bounds.centerX(), bounds.centerY(), min(bounds.width(), bounds.height()) / 2f)

        fillPaint.color = palette.dial
        canvas.drawCircle(dial.cx, dial.cy, dial.radius, fillPaint)

        drawTicks(canvas, dial)
        drawNumerals(canvas, dial)

        textPaint.color = palette.secondaryText
        textPaint.textSize = dial.radius * Analog.DATE_SIZE
        val date = now.format(analogDateFormatter).uppercase(locale)
        drawCenteredText(canvas, date, dial.cx, dial.cy + dial.radius * Analog.DATE_DISTANCE)

        val time = now.toLocalTime()
        drawRadialLine(canvas, dial, ClockMath.hourHandDegrees(time), Analog.HOUR_HAND, palette.hands)
        drawRadialLine(canvas, dial, ClockMath.minuteHandDegrees(time), Analog.MINUTE_HAND, palette.hands)
        drawRadialLine(canvas, dial, ClockMath.secondHandDegrees(time), Analog.SECOND_HAND, palette.accent)

        fillPaint.color = palette.accent
        canvas.drawCircle(dial.cx, dial.cy, dial.radius * Analog.CENTER_CAP_RADIUS, fillPaint)
    }

    private fun drawTicks(canvas: Canvas, dial: Dial) {
        for (tick in 0 until TICKS_PER_TURN) {
            val isHourTick = tick % 5 == 0
            drawRadialLine(
                canvas = canvas,
                dial = dial,
                degrees = tick * 360f / TICKS_PER_TURN,
                line = if (isHourTick) Analog.HOUR_TICK else Analog.MINUTE_TICK,
                color = if (isHourTick) palette.hands else palette.ticks,
            )
        }
    }

    private fun drawNumerals(canvas: Canvas, dial: Dial) {
        textPaint.color = palette.primaryText
        textPaint.textSize = dial.radius * Analog.NUMERAL_SIZE
        val distance = dial.radius * Analog.NUMERAL_DISTANCE
        for (hour in 1..12) {
            // Rotating the canvas would tilt the numerals, so their positions come from sin and cos.
            val angle = Math.toRadians(hour * 30.0)
            val x = dial.cx + distance * sin(angle).toFloat()
            val y = dial.cy - distance * cos(angle).toFloat()
            drawCenteredText(canvas, hour.toString(), x, y)
        }
    }

    private fun drawRadialLine(
        canvas: Canvas,
        dial: Dial,
        degrees: Float,
        line: RadialLine,
        @ColorInt color: Int,
    ) {
        strokePaint.color = color
        strokePaint.strokeWidth = dial.radius * line.width
        canvas.withRotation(degrees, dial.cx, dial.cy) {
            drawLine(
                dial.cx, dial.cy - dial.radius * line.start,
                dial.cx, dial.cy - dial.radius * line.end,
                strokePaint,
            )
        }
    }

    private fun drawDigital(canvas: Canvas, bounds: RectF, now: LocalDateTime, use24Hour: Boolean) {
        val time = now.format(if (use24Hour) time24Formatter else time12Formatter)
        val seconds = now.format(secondsFormatter)
        val amPm = if (use24Hour) null else now.format(amPmFormatter)
        val date = now.format(digitalDateFormatter)

        // Widths here are multiples of the time's text size. Text width scales with text size,
        // so one measurement at a reference size tells us the largest size that fits.
        val timeWidth = relativeWidth(time, 1f)
        val sideWidth = maxOf(
            relativeWidth(seconds, Digital.SIDE_SIZE),
            amPm?.let { relativeWidth(it, Digital.SIDE_SIZE) } ?: 0f,
        )
        val blockWidth = timeWidth + Digital.SIDE_GAP + sideWidth
        val timeSize = min(
            bounds.width() * Digital.FILL / blockWidth,
            bounds.height() * Digital.FILL / Digital.BLOCK_HEIGHT,
        )
        val sideSize = timeSize * Digital.SIDE_SIZE

        val left = bounds.centerX() - blockWidth * timeSize / 2f
        val sideLeft = left + (timeWidth + Digital.SIDE_GAP) * timeSize
        val timeBaseline = bounds.centerY() + (Digital.DIGIT_HEIGHT - Digital.BLOCK_HEIGHT / 2f) * timeSize
        // Seconds share the time's baseline. AM/PM lines up with the top of the digits.
        val amPmBaseline = timeBaseline - (timeSize - sideSize) * Digital.DIGIT_HEIGHT
        val dateBaseline = timeBaseline + (Digital.DATE_GAP + Digital.DATE_SIZE) * timeSize

        textPaint.textAlign = Paint.Align.LEFT
        drawText(canvas, time, left, timeBaseline, timeSize, palette.primaryText)
        drawText(canvas, seconds, sideLeft, timeBaseline, sideSize, palette.accent)
        if (amPm != null) {
            drawText(canvas, amPm, sideLeft, amPmBaseline, sideSize, palette.secondaryText)
        }

        textPaint.textAlign = Paint.Align.CENTER
        val maxDateWidth = bounds.width() * Digital.FILL
        textPaint.textSize = timeSize * Digital.DATE_SIZE
        val dateWidth = textPaint.measureText(date)
        if (dateWidth > maxDateWidth) {
            textPaint.textSize *= maxDateWidth / dateWidth
        }
        textPaint.color = palette.secondaryText
        canvas.drawText(date, bounds.centerX(), dateBaseline, textPaint)
    }

    private fun relativeWidth(text: String, relativeSize: Float): Float {
        textPaint.textSize = REFERENCE_TEXT_SIZE * relativeSize
        return textPaint.measureText(text) / REFERENCE_TEXT_SIZE
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, baseline: Float, size: Float, @ColorInt color: Int) {
        textPaint.textSize = size
        textPaint.color = color
        canvas.drawText(text, x, baseline, textPaint)
    }

    private fun drawCenteredText(canvas: Canvas, text: String, x: Float, y: Float) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        canvas.drawText(text, x, y - textBounds.exactCenterY(), textPaint)
    }

    private class Dial(val cx: Float, val cy: Float, val radius: Float)

    // A line along a radius of the dial, from start to end. A negative start reaches past the
    // center, which gives the hands their short tail.
    private class RadialLine(val start: Float, val end: Float, val width: Float)

    // Analog sizes and distances are fractions of the dial radius.
    private object Analog {
        val HOUR_HAND = RadialLine(start = -0.08f, end = 0.5f, width = 0.05f)
        val MINUTE_HAND = RadialLine(start = -0.08f, end = 0.76f, width = 0.032f)
        val SECOND_HAND = RadialLine(start = -0.16f, end = 0.86f, width = 0.012f)
        val HOUR_TICK = RadialLine(start = 0.84f, end = 0.94f, width = 0.025f)
        val MINUTE_TICK = RadialLine(start = 0.89f, end = 0.94f, width = 0.01f)
        const val NUMERAL_DISTANCE = 0.69f
        const val NUMERAL_SIZE = 0.14f
        const val DATE_DISTANCE = 0.4f
        const val DATE_SIZE = 0.09f
        const val CENTER_CAP_RADIUS = 0.035f
    }

    // Digital sizes and distances are multiples of the time's text size.
    private object Digital {
        const val SIDE_SIZE = 0.3f
        const val SIDE_GAP = 0.06f
        const val DIGIT_HEIGHT = 0.72f
        const val DATE_SIZE = 0.16f
        const val DATE_GAP = 0.28f
        const val BLOCK_HEIGHT = DIGIT_HEIGHT + DATE_GAP + DATE_SIZE

        // The face uses at most this fraction of the bounds' width and height. The rest is margin.
        const val FILL = 0.9f
    }

    private companion object {
        const val TICKS_PER_TURN = 60
        const val REFERENCE_TEXT_SIZE = 100f
    }
}
