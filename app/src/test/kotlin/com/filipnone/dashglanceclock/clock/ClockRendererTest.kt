package com.filipnone.dashglanceclock.clock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ClockRendererTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val renderer = ClockRenderer(Locale.US)

    @Test
    fun analogFace_secondHandPointsAtTheCurrentSecond() {
        for (second in listOf(15, 45)) {
            val time = at(second = second)
            val bitmap = renderAnalog(time)

            val degrees = ClockMath.secondHandDegrees(time.toLocalTime())
            assertEquals("second $second", ClockPalette.Dark.accent, pixelOnDial(bitmap, degrees, distance = 0.6f))
        }
    }

    @Test
    fun analogFace_drawsTheCenterCapOnTopOfTheHands() {
        val bitmap = renderAnalog(at(second = 0))

        assertEquals(ClockPalette.Dark.accent, bitmap.getPixel(DIAL_CX.toInt(), DIAL_CY.toInt()))
    }

    @Test
    fun analogFace_leavesMostOfTheDialEmpty() {
        val bitmap = renderAnalog(at(second = 30))

        val samples = (0 until 360).map { pixelOnDial(bitmap, it.toFloat(), distance = 0.55f) }
        val dialShare = samples.count { it == ClockPalette.Dark.dial } / samples.size.toFloat()
        assertTrue("dial color covers only $dialShare of the ring", dialShare > 0.9f)
    }

    @Test
    fun everyAnalogDesign_staysInsideItsDial() {
        // A 600 x 400 face in the middle of a larger canvas, so a wide rectangular dial has
        // room to spill over its sides.
        val bounds = RectF(50f, 50f, 650f, 450f)
        for (design in ClockDesigns.analog) {
            val halfHeight = if (design.dial.shape == DialShape.RECTANGLE) {
                minOf(bounds.height(), bounds.width() / design.dial.aspect) / 2f
            } else {
                DIAL_RADIUS
            }
            val halfWidth = if (design.dial.shape == DialShape.RECTANGLE) halfHeight * design.dial.aspect else halfHeight
            for (isDarkMode in listOf(false, true)) {
                val palette = design.palette(context, isDarkMode)
                val bitmap = render(700, 500, bounds, design, palette, at(second = 30))

                val outside = countInk(bitmap, palette.background) { x, y ->
                    val dx = abs(x - bounds.centerX())
                    val dy = abs(y - bounds.centerY())
                    when (design.dial.shape) {
                        DialShape.SQUIRCLE, DialShape.RECTANGLE -> dx > halfWidth + 1f || dy > halfHeight + 1f
                        else -> hypot(dx, dy) > halfHeight + 1f
                    }
                }
                assertEquals("${design.id} (dark=$isDarkMode)", 0, outside)
            }
        }
    }

    @Test
    fun everyDigitalDesign_staysInsideItsBounds() {
        val shapes = listOf(
            Triple(800, 400, RectF(100f, 60f, 700f, 340f)),
            Triple(400, 800, RectF(60f, 100f, 340f, 700f)),
        )
        val times = listOf(at(second = 30), LocalDateTime.of(2026, 11, 18, 8, 59, 7))
        for (design in ClockDesigns.digital) {
            val palette = design.palette(context, isDarkMode = true)
            for ((width, height, bounds) in shapes) {
                for (time in times) {
                    for (use24Hour in listOf(true, false)) {
                        val bitmap = render(width, height, bounds, design, palette, time, use24Hour)

                        val case = "${design.id} ${width}x$height $time 24h=$use24Hour"
                        val inside = countInk(bitmap, palette.background) { x, y -> bounds.contains(x, y) }
                        // Glows fade out slowly, so their last faint pixels don't count as ink.
                        val outside = countInk(bitmap, palette.background, tolerance = 24) { x, y ->
                            !bounds.contains(x, y)
                        }
                        assertTrue("nothing drawn: $case", inside > 0)
                        assertEquals("ink outside bounds: $case", 0, outside)
                    }
                }
            }
        }
    }

    @Test
    fun animatedDesigns_moveWithinASecond_andStayInsideTheirBounds() {
        val bounds = RectF(100f, 60f, 700f, 340f)
        for (design in ClockDesigns.digital.filter { it.motion == Motion.CONTINUOUS }) {
            val palette = design.palette(context, isDarkMode = true)
            // Three seconds of frames, so at least one glitch burst falls inside.
            val frames = (0 until 75).map { frame ->
                val time = at(second = 30).plusNanos(frame * SecondTicker.ANIMATION_FRAME_MILLIS * 1_000_000L)
                render(800, 400, bounds, design, palette, time)
            }
            val distinct = frames.drop(1).count { !it.sameAs(frames.first()) }
            assertTrue("${design.id} never changes", distinct > 0)
            for ((index, frame) in frames.withIndex()) {
                val outside = countInk(frame, palette.background, tolerance = 24) { x, y -> !bounds.contains(x, y) }
                assertEquals("${design.id} frame $index", 0, outside)
            }
        }
    }

    @Test
    fun flipClock_turnsItsCardsWhenTheMinuteChanges() {
        val design = ClockDesigns.digital.single { it.id == "flip_clock" }
        val palette = design.palette(context, isDarkMode = true)
        val bounds = RectF(100f, 60f, 700f, 340f)
        fun frame(time: LocalDateTime) = render(800, 400, bounds, design, palette, time, use24Hour = false)
        val minute = LocalDateTime.of(2026, 11, 20, 10, 0, 0)
        val before = frame(minute.minusSeconds(30))
        val after = frame(minute.plusSeconds(30))

        var millis = 0L
        while (millis < Motion.FLIP_MILLIS) {
            val turning = frame(minute.plusNanos(millis * 1_000_000L))
            if (millis > 0L) {
                assertFalse("${millis}ms looks like 9:59", turning.sameAs(before))
                assertFalse("${millis}ms looks like 10:00", turning.sameAs(after))
            }
            val outside = countInk(turning, palette.background, tolerance = 24) { x, y -> !bounds.contains(x, y) }
            assertEquals("${millis}ms", 0, outside)
            millis += SecondTicker.ANIMATION_FRAME_MILLIS
        }
        assertTrue(frame(minute.plusNanos(Motion.FLIP_MILLIS * 1_000_000L)).sameAs(after))
    }

    @Test
    fun blinkingColon_showsOnEvenSecondsOnly() {
        val design = ClockDesigns.digital.first { it.id == "lagonda_1976" }
        val palette = design.palette(context, isDarkMode = true)
        val bounds = RectF(0f, 0f, 800f, 400f)

        val even = render(800, 400, bounds, design, palette, at(second = 30))
        val odd = render(800, 400, bounds, design, palette, at(second = 31))

        assertFalse(even.sameAs(odd))
    }

    @Test
    fun noTwoDesignsLookTheSame_evenInTheSameColors() {
        val bounds = RectF(0f, 0f, 600f, 400f)
        for (designs in listOf(ClockDesigns.analog, ClockDesigns.digital)) {
            val renders = designs.associateWith { render(600, 400, bounds, it, ClockPalette.Dark, at(second = 30)) }
            for ((first, firstBitmap) in renders) {
                for ((second, secondBitmap) in renders) {
                    if (first.id < second.id) {
                        assertFalse("${first.id} and ${second.id}", firstBitmap.sameAs(secondBitmap))
                    }
                }
            }
        }
    }

    @Test
    fun emptyBounds_drawNothing() {
        for (design in ClockDesigns.all) {
            val bitmap = render(100, 100, RectF(), design, ClockPalette.Dark, at(second = 30))
            assertEquals(design.id, 0, countInk(bitmap, ClockPalette.Dark.background) { _, _ -> true })
        }
    }

    private fun at(second: Int): LocalDateTime = LocalDateTime.of(2026, 11, 17, 22, 10, second)

    private fun renderAnalog(time: LocalDateTime): Bitmap =
        render(
            width = 600,
            height = 400,
            bounds = RectF(0f, 0f, 600f, 400f),
            design = ClockDesigns.DashGlanceAnalog,
            palette = ClockPalette.Dark,
            time = time,
        )

    private fun render(
        width: Int,
        height: Int,
        bounds: RectF,
        design: ClockDesign,
        palette: ClockPalette,
        time: LocalDateTime,
        use24Hour: Boolean = true,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        renderer.palette = palette
        renderer.drawBackground(canvas)
        renderer.draw(canvas, bounds, time, design, use24Hour)
        return bitmap
    }

    private fun pixelOnDial(bitmap: Bitmap, degrees: Float, distance: Float): Int {
        val angle = Math.toRadians(degrees.toDouble())
        val x = DIAL_CX + DIAL_RADIUS * distance * sin(angle)
        val y = DIAL_CY - DIAL_RADIUS * distance * cos(angle)
        return bitmap.getPixel(x.roundToInt(), y.roundToInt())
    }

    // Counts the pixels that differ from the background by more than the tolerance, in any
    // channel, and whose center passes the filter.
    private fun countInk(
        bitmap: Bitmap,
        background: Int,
        tolerance: Int = 0,
        where: (x: Float, y: Float) -> Boolean,
    ): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var count = 0
        for ((index, pixel) in pixels.withIndex()) {
            val differs = abs(Color.red(pixel) - Color.red(background)) > tolerance ||
                abs(Color.green(pixel) - Color.green(background)) > tolerance ||
                abs(Color.blue(pixel) - Color.blue(background)) > tolerance
            if (differs && where(index % bitmap.width + 0.5f, index / bitmap.width + 0.5f)) count++
        }
        return count
    }

    private companion object {
        // The analog face fills the largest circle that fits a 600 x 400 canvas.
        const val DIAL_CX = 300f
        const val DIAL_CY = 200f
        const val DIAL_RADIUS = 200f
    }
}
