package com.filipnone.dashglanceclock.clock

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withRotation
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal class AnalogFace(private val locale: Locale) {

    private val glyphs = GlyphPainters()
    private val dates = DateFormats(locale)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()
    private val shineRotation = Matrix()

    fun draw(canvas: Canvas, bounds: RectF, now: LocalDateTime, design: AnalogDesign, palette: ClockPalette) {
        val dial = Dial.fit(bounds, design.dial)

        drawDial(canvas, dial, design.dial, palette)
        design.rings.forEach { drawRing(canvas, dial, it, palette) }
        design.band?.let { drawBand(canvas, dial, it, palette) }
        drawMarks(canvas, dial, design, palette)
        design.numerals?.let { drawNumerals(canvas, dial, it, palette) }
        design.legend?.let { drawDialText(canvas, dial, it.text, it.look, palette) }
        design.date?.let { drawDialText(canvas, dial, dates.format(now, it.style), it, palette) }

        val time = now.toLocalTime()
        drawHand(canvas, dial, ClockMath.hourHandDegrees(time), design.hourHand, palette)
        drawHand(canvas, dial, ClockMath.minuteHandDegrees(time), design.minuteHand, palette)
        design.hubCap?.let { drawCap(canvas, dial, it, palette) }
        design.secondHand?.let { drawSecondHand(canvas, dial, ClockMath.secondHandDegrees(time), it, palette) }
        design.caps.forEach { drawCap(canvas, dial, it, palette) }
        design.knob?.let {
            val color = palette.color(it.ink)
            drawDisc(canvas, dial.cx, dial.cy + dial.radius * it.distance, dial.radius * it.radius, color, Finish.DOME)
        }
    }

    private fun drawDial(canvas: Canvas, dial: Dial, style: DialStyle, palette: ClockPalette) {
        if (style.bezel > 0f) {
            if (style.shape == DialShape.RECTANGLE && style.corner > 0f) {
                drawCase(canvas, dial, style, palette)
            } else if (style.shape == DialShape.RECTANGLE) {
                drawFrame(canvas, dial, palette)
            } else {
                fill.color = palette.bezel
                if (palette.bezelShade != palette.bezel) fill.shader = shine(dial, palette.bezel, palette.bezelShade)
                drawShape(canvas, style.shape, dial.cx, dial.cy, dial.outerHalfWidth, dial.outerHalfHeight)
                fill.shader = null
            }
        }

        fill.color = palette.dial
        if (palette.dialEdge != palette.dial) {
            fill.shader = RadialGradient(
                dial.cx, dial.cy, dial.radius, palette.dial, palette.dialEdge, Shader.TileMode.CLAMP,
            )
        }
        val corner = dial.outerHalfHeight * style.corner * DIAL_CORNER
        drawShape(canvas, style.shape, dial.cx, dial.cy, dial.halfWidth, dial.radius, corner)
        fill.shader = null
    }

    // A molded plastic case. The darker band just outside the dial is the inside wall of the
    // case, seen through the crystal.
    private fun drawCase(canvas: Canvas, dial: Dial, style: DialStyle, palette: ClockPalette) {
        val corner = dial.outerHalfHeight * style.corner
        fill.color = palette.bezel
        canvas.drawRoundRect(
            dial.cx - dial.outerHalfWidth, dial.cy - dial.outerHalfHeight,
            dial.cx + dial.outerHalfWidth, dial.cy + dial.outerHalfHeight,
            corner, corner, fill,
        )
        if (palette.bezelShade != palette.bezel) {
            val wall = (dial.outerHalfHeight - dial.radius) * CASE_WALL
            val wallCorner = corner * DIAL_CORNER + wall
            fill.color = palette.bezelShade
            canvas.drawRoundRect(
                dial.cx - dial.halfWidth - wall, dial.cy - dial.radius - wall,
                dial.cx + dial.halfWidth + wall, dial.cy + dial.radius + wall,
                wallCorner, wallCorner, fill,
            )
        }
    }

    // A picture frame with mitred corners. Light falls from the top left, so the top side is
    // the brightest and the bottom the darkest.
    private fun drawFrame(canvas: Canvas, dial: Dial, palette: ClockPalette) {
        val outerLeft = dial.cx - dial.outerHalfWidth
        val outerRight = dial.cx + dial.outerHalfWidth
        val outerTop = dial.cy - dial.outerHalfHeight
        val outerBottom = dial.cy + dial.outerHalfHeight
        val innerLeft = dial.cx - dial.halfWidth
        val innerRight = dial.cx + dial.halfWidth
        val innerTop = dial.cy - dial.radius
        val innerBottom = dial.cy + dial.radius
        val light = palette.bezel
        val shade = palette.bezelShade
        val sides = listOf(
            floatArrayOf(outerLeft, outerTop, outerRight, outerTop, innerRight, innerTop, innerLeft, innerTop) to light,
            floatArrayOf(outerLeft, outerBottom, outerLeft, outerTop, innerLeft, innerTop, innerLeft, innerBottom) to
                ColorUtils.blendARGB(light, shade, 0.3f),
            floatArrayOf(outerRight, outerTop, outerRight, outerBottom, innerRight, innerBottom, innerRight, innerTop) to
                ColorUtils.blendARGB(light, shade, 0.7f),
            floatArrayOf(outerRight, outerBottom, outerLeft, outerBottom, innerLeft, innerBottom, innerRight, innerBottom) to
                shade,
        )
        for ((corners, color) in sides) {
            path.reset()
            path.moveTo(corners[0], corners[1])
            for (i in 2 until corners.size step 2) path.lineTo(corners[i], corners[i + 1])
            path.close()
            fill.color = color
            canvas.drawPath(path, fill)
        }
    }

    private fun drawShape(
        canvas: Canvas,
        shape: DialShape,
        cx: Float,
        cy: Float,
        halfWidth: Float,
        halfHeight: Float,
        corner: Float = 0f,
    ) {
        when (shape) {
            DialShape.CIRCLE -> canvas.drawCircle(cx, cy, halfHeight, fill)
            DialShape.SQUIRCLE -> {
                val corner = halfHeight * SQUIRCLE_CORNER
                canvas.drawRoundRect(cx - halfWidth, cy - halfHeight, cx + halfWidth, cy + halfHeight, corner, corner, fill)
            }
            DialShape.RECTANGLE ->
                canvas.drawRoundRect(cx - halfWidth, cy - halfHeight, cx + halfWidth, cy + halfHeight, corner, corner, fill)
            DialShape.SCALLOP -> {
                path.reset()
                for (point in 0 until SCALLOP_POINTS) {
                    val angle = 2.0 * PI * point / SCALLOP_POINTS
                    val wave = (1.0 + cos(SCALLOP_LOBES * angle)) / 2.0
                    val r = halfHeight * (1f - SCALLOP_DEPTH + SCALLOP_DEPTH * wave.toFloat())
                    val x = cx + r * sin(angle).toFloat()
                    val y = cy - r * cos(angle).toFloat()
                    if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, fill)
            }
        }
    }

    private fun drawRing(canvas: Canvas, dial: Dial, ring: Ring, palette: ClockPalette) {
        val color = palette.color(ring.ink)
        stroke.color = color
        stroke.alpha = (Color.alpha(color) * ring.alpha).roundToInt()
        stroke.strokeWidth = dial.radius * ring.width
        val radius = dial.radius * ring.distance
        if (ring.dots > 0) {
            // A dash of length zero with a round cap is a dot.
            stroke.strokeCap = Paint.Cap.ROUND
            stroke.pathEffect = DashPathEffect(floatArrayOf(0f, (2.0 * PI * radius / ring.dots).toFloat()), 0f)
        }
        canvas.drawCircle(dial.cx, dial.cy, radius, stroke)
        stroke.pathEffect = null
    }

    // Brushed metal: a little lighter at the top edge and darker at the bottom.
    private fun drawBand(canvas: Canvas, dial: Dial, band: Band, palette: ClockPalette) {
        val halfWidth = dial.halfWidth * band.width
        val halfHeight = dial.radius * band.height
        val color = palette.color(band.ink)
        fill.shader = LinearGradient(
            0f, dial.cy - halfHeight, 0f, dial.cy + halfHeight,
            intArrayOf(ColorUtils.blendARGB(color, Color.WHITE, 0.3f), color, ColorUtils.blendARGB(color, Color.BLACK, 0.2f)),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(dial.cx - halfWidth, dial.cy - halfHeight, dial.cx + halfWidth, dial.cy + halfHeight, fill)
        fill.shader = null
    }

    private fun drawMarks(canvas: Canvas, dial: Dial, design: AnalogDesign, palette: ClockPalette) {
        for (tick in 0 until TICKS_PER_TURN) {
            val mark = when {
                tick % 5 != 0 -> design.minuteMarks
                tick == 0 -> design.twelveMark
                tick % 15 == 0 -> design.quarterMarks
                else -> design.hourMarks
            } ?: continue
            drawMark(canvas, dial, tick * 360f / TICKS_PER_TURN, mark, palette)
        }
    }

    private fun drawMark(canvas: Canvas, dial: Dial, degrees: Float, mark: Mark, palette: ClockPalette) {
        val r = dial.radius
        val reach = dial.edge(degrees)
        canvas.withRotation(degrees, dial.cx, dial.cy) {
            when (mark) {
                is Mark.Line -> {
                    stroke.color = palette.color(mark.ink)
                    stroke.strokeWidth = r * mark.width
                    stroke.strokeCap = if (mark.round) Paint.Cap.ROUND else Paint.Cap.BUTT
                    drawLine(dial.cx, dial.cy - reach * mark.start, dial.cx, dial.cy - reach * mark.end, stroke)
                }
                is Mark.Dot -> {
                    fill.color = palette.color(mark.ink)
                    drawCircle(dial.cx, dial.cy - reach * mark.distance, r * mark.radius, fill)
                }
                is Mark.Triangle -> {
                    fill.color = palette.color(mark.ink)
                    path.reset()
                    path.moveTo(dial.cx, dial.cy - reach * mark.start)
                    path.lineTo(dial.cx + r * mark.width / 2f, dial.cy - reach * mark.end)
                    path.lineTo(dial.cx - r * mark.width / 2f, dial.cy - reach * mark.end)
                    path.close()
                    drawPath(path, fill)
                }
                is Mark.Square -> {
                    fill.color = palette.color(mark.ink)
                    val half = r * mark.size / 2f
                    val center = dial.cy - reach * mark.distance
                    drawRect(dial.cx - half, center - half, dial.cx + half, center + half, fill)
                }
            }
        }
    }

    private fun drawNumerals(canvas: Canvas, dial: Dial, numerals: Numerals, palette: ClockPalette) {
        val painter = glyphs[numerals.glyphs]
        val height = dial.radius * numerals.height
        val color = palette.color(numerals.ink)
        for (hour in 1..12) {
            if (numerals.set == NumeralSet.QUARTERS && hour % 3 != 0) continue
            val text = if (numerals.roman) ROMAN_NUMERALS[hour - 1] else hour.toString()
            // Rotating the canvas would tilt the numerals, so their positions come from sin and cos.
            val distance = dial.edge(hour * 30f) * numerals.distance
            val angle = Math.toRadians(hour * 30.0)
            val x = dial.cx + distance * sin(angle).toFloat()
            val y = dial.cy - distance * cos(angle).toFloat()
            val left = x - painter.width(text, height) / 2f
            painter.draw(canvas, text, left, y + height / 2f, height, color, palette.unlit)
        }
    }

    private fun drawDialText(canvas: Canvas, dial: Dial, text: String, look: DialText, palette: ClockPalette) {
        val painter = glyphs.font(look.font)
        val shown = if (look.uppercase) text.uppercase(locale) else text
        val maxWidth = dial.radius * DIAL_TEXT_MAX_WIDTH
        var height = dial.radius * look.size
        val width = painter.width(shown, height)
        if (width > maxWidth) height *= maxWidth / width
        val left = dial.cx - painter.width(shown, height) / 2f
        val baseline = dial.cy + dial.radius * look.distance + height / 2f
        painter.draw(canvas, shown, left, baseline, height, palette.color(look.ink))
    }

    private fun drawHand(canvas: Canvas, dial: Dial, degrees: Float, hand: Hand, palette: ClockPalette) {
        val cx = dial.cx
        val cy = dial.cy
        val length = dial.radius * hand.length
        val width = dial.radius * hand.width
        val tail = dial.radius * hand.tail
        fill.color = palette.color(hand.ink)
        stroke.color = fill.color

        canvas.withRotation(degrees, cx, cy) {
            when (hand.shape) {
                HandShape.LINE -> drawStem(cx, cy + tail, cy - length, width)
                HandShape.BATON -> drawRect(cx - width / 2f, cy - length, cx + width / 2f, cy + tail, fill)
                HandShape.NEEDLE -> {
                    path.reset()
                    path.moveTo(cx - width / 2f, cy + tail)
                    path.lineTo(cx + width / 2f, cy + tail)
                    path.lineTo(cx + width * NEEDLE_TIP, cy - length)
                    path.lineTo(cx - width * NEEDLE_TIP, cy - length)
                    path.close()
                    drawPath(path, fill)
                }
                HandShape.PENCIL -> {
                    val shoulder = cy - length + width * PENCIL_POINT
                    path.reset()
                    path.moveTo(cx - width / 2f, cy + tail)
                    path.lineTo(cx + width / 2f, cy + tail)
                    path.lineTo(cx + width / 2f, shoulder)
                    path.lineTo(cx, cy - length)
                    path.lineTo(cx - width / 2f, shoulder)
                    path.close()
                    drawPath(path, fill)
                }
                HandShape.LEAF -> {
                    path.reset()
                    path.moveTo(cx, cy + tail)
                    path.quadTo(cx + width, cy - length * LEAF_WIDEST, cx, cy - length)
                    path.quadTo(cx - width, cy - length * LEAF_WIDEST, cx, cy + tail)
                    path.close()
                    drawPath(path, fill)
                }
                HandShape.NECKED -> {
                    val neck = dial.radius * NECK_LENGTH
                    drawStem(cx, cy + tail, cy - neck, width * NECK_WIDTH)
                    drawStem(cx, cy - neck - width / 2f, cy - length + width / 2f, width)
                }
                HandShape.SPADE -> {
                    val stem = width * SPADE_STEM
                    val half = width / 2f
                    val base = cy - length * SPADE_FROM
                    val widest = cy - length * SPADE_WIDEST
                    val dropEnd = cy - length * SPADE_TO
                    // The sides leave the widest point straight up, then bend in early, so
                    // the drop narrows back to the stem's width. A fine point carries on to the tip.
                    val shoulder = widest - (widest - dropEnd) * SPADE_SHOULDER
                    path.reset()
                    path.moveTo(cx - stem / 2f, cy + tail)
                    path.lineTo(cx - stem / 2f, base)
                    path.quadTo(cx - half, base, cx - half, widest)
                    path.quadTo(cx - half, shoulder, cx - stem / 2f, dropEnd)
                    path.lineTo(cx, cy - length)
                    path.lineTo(cx + stem / 2f, dropEnd)
                    path.quadTo(cx + half, shoulder, cx + half, widest)
                    path.quadTo(cx + half, base, cx + stem / 2f, base)
                    path.lineTo(cx + stem / 2f, cy + tail)
                    path.close()
                    drawPath(path, fill)
                }
                HandShape.LANCE, HandShape.ARROW -> {
                    val widest = length * if (hand.shape == HandShape.LANCE) LANCE_WIDEST else ARROW_WIDEST
                    val base = width * TAPER_BASE
                    path.reset()
                    path.moveTo(cx - base / 2f, cy + tail)
                    path.lineTo(cx - width / 2f, cy - widest)
                    path.lineTo(cx, cy - length)
                    path.lineTo(cx + width / 2f, cy - widest)
                    path.lineTo(cx + base / 2f, cy + tail)
                    path.close()
                    drawPath(path, fill)
                }
            }
            hand.lume?.let {
                stroke.color = palette.color(it)
                drawStem(cx, cy - length * LUME_FROM, cy - length * LUME_TO, width * LUME_WIDTH)
            }
        }
    }

    private fun drawSecondHand(canvas: Canvas, dial: Dial, degrees: Float, hand: SecondHand, palette: ClockPalette) {
        val r = dial.radius
        canvas.withRotation(degrees, dial.cx, dial.cy) {
            when (hand) {
                is SecondHand.Line -> {
                    stroke.color = palette.color(hand.ink)
                    drawStem(dial.cx, dial.cy + r * hand.tail, dial.cy - r * hand.length, r * hand.width)
                    if (hand.counterweight > 0f) {
                        fill.color = stroke.color
                        drawCircle(dial.cx, dial.cy + r * hand.tail, r * hand.counterweight, fill)
                    }
                }
                is SecondHand.Orbit -> {
                    fill.color = palette.color(hand.ink)
                    val center = dial.cy - r * hand.distance
                    val size = r * hand.size
                    if (hand.triangle) {
                        path.reset()
                        path.moveTo(dial.cx, center - size)
                        path.lineTo(dial.cx + size, center + size)
                        path.lineTo(dial.cx - size, center + size)
                        path.close()
                        drawPath(path, fill)
                    } else {
                        drawCircle(dial.cx, center, size, fill)
                    }
                }
            }
        }
    }

    private fun drawCap(canvas: Canvas, dial: Dial, cap: Cap, palette: ClockPalette) {
        drawDisc(canvas, dial.cx, dial.cy, dial.radius * cap.radius, palette.color(cap.ink), cap.finish)
    }

    // A dome catches the light at its top left. A cone is brushed in circles, so it has two
    // bright and two dark sides, the same as a chrome bezel.
    private fun drawDisc(canvas: Canvas, cx: Float, cy: Float, radius: Float, @ColorInt color: Int, finish: Finish) {
        fill.color = color
        fill.shader = when (finish) {
            Finish.FLAT -> null
            Finish.DOME -> RadialGradient(
                cx - radius * DOME_HIGHLIGHT, cy - radius * DOME_HIGHLIGHT, radius * DOME_SPREAD,
                intArrayOf(ColorUtils.blendARGB(color, Color.WHITE, 0.7f), color, ColorUtils.blendARGB(color, Color.BLACK, 0.5f)),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            Finish.CONE -> shine(
                Dial(cx, cy, radius, radius, radius, radius),
                ColorUtils.blendARGB(color, Color.WHITE, 0.45f),
                ColorUtils.blendARGB(color, Color.BLACK, 0.45f),
            )
        }
        canvas.drawCircle(cx, cy, radius, fill)
        fill.shader = null
    }

    private fun shine(dial: Dial, @ColorInt light: Int, @ColorInt shade: Int): Shader =
        SweepGradient(dial.cx, dial.cy, intArrayOf(light, shade, light, shade, light), null).apply {
            shineRotation.setRotate(SHINE_DEGREES, dial.cx, dial.cy)
            setLocalMatrix(shineRotation)
        }

    private fun Canvas.drawStem(x: Float, fromY: Float, toY: Float, width: Float) {
        stroke.strokeWidth = width
        stroke.strokeCap = Paint.Cap.ROUND
        drawLine(x, fromY, x, toY, stroke)
    }

    // The radius is the dial's half height, so hands and marks keep their size on a wide
    // rectangular dial. Half widths only differ from it on rectangles.
    private class Dial(
        val cx: Float,
        val cy: Float,
        val radius: Float,
        val halfWidth: Float,
        val outerHalfWidth: Float,
        val outerHalfHeight: Float,
        val rectangle: Boolean = false,
    ) {

        // Distance from the center to the dial's edge in the direction of a clock angle. Marks
        // and numerals on a rectangular dial keep to the edge, so they reach into the corners.
        fun edge(degrees: Float): Float {
            if (!rectangle) return radius
            val angle = Math.toRadians(degrees.toDouble())
            val across = abs(sin(angle)).toFloat()
            val up = abs(cos(angle)).toFloat()
            val toSide = if (across > EPSILON) halfWidth / across else Float.MAX_VALUE
            val toTop = if (up > EPSILON) radius / up else Float.MAX_VALUE
            return min(toSide, toTop)
        }

        companion object {
            private const val EPSILON = 1e-4f

            fun fit(bounds: RectF, style: DialStyle): Dial {
                val rectangle = style.shape == DialShape.RECTANGLE
                val aspect = if (rectangle) style.aspect else 1f
                val outerHalfHeight = min(bounds.height() / 2f, bounds.width() / 2f / aspect)
                val outerHalfWidth = outerHalfHeight * aspect
                val bezel = outerHalfHeight * style.bezel
                return Dial(
                    cx = bounds.centerX(),
                    cy = bounds.centerY(),
                    radius = outerHalfHeight - bezel,
                    halfWidth = outerHalfWidth - bezel,
                    outerHalfWidth = outerHalfWidth,
                    outerHalfHeight = outerHalfHeight,
                    rectangle = rectangle,
                )
            }
        }
    }

    private companion object {
        const val TICKS_PER_TURN = 60
        val ROMAN_NUMERALS = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")

        // A sweep gradient starts at 3 o'clock. Turning it back 45 degrees puts the chrome's
        // highlights at half past one and half past seven.
        const val SHINE_DEGREES = -45f
        const val SQUIRCLE_CORNER = 0.45f
        const val SCALLOP_POINTS = 240
        const val SCALLOP_LOBES = 12
        const val SCALLOP_DEPTH = 0.07f
        const val DIAL_TEXT_MAX_WIDTH = 1f
        const val DIAL_CORNER = 0.2f
        const val CASE_WALL = 0.2f
        const val DOME_HIGHLIGHT = 0.35f
        const val DOME_SPREAD = 1.5f

        const val NEEDLE_TIP = 0.05f
        const val PENCIL_POINT = 1.2f
        const val LEAF_WIDEST = 0.4f
        const val NECK_LENGTH = 0.12f
        const val NECK_WIDTH = 0.35f
        const val SPADE_STEM = 0.2f
        const val SPADE_FROM = 0.5f
        const val SPADE_WIDEST = 0.58f
        const val SPADE_TO = 0.78f
        const val SPADE_SHOULDER = 0.3f
        const val LANCE_WIDEST = 0.72f
        const val ARROW_WIDEST = 0.92f
        const val TAPER_BASE = 0.3f
        const val LUME_FROM = 0.42f
        const val LUME_TO = 0.86f
        const val LUME_WIDTH = 0.5f
    }
}
