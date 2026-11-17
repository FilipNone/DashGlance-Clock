package com.filipnone.dashglanceclock.clock

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ClockRendererTest {

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
    fun analogFace_drawsNothingOutsideTheDial() {
        val bitmap = renderAnalog(at(second = 30))

        val outside = countInk(bitmap) { x, y -> hypot(x - DIAL_CX, y - DIAL_CY) > DIAL_RADIUS + 1f }
        assertEquals(0, outside)
    }

    @Test
    fun digitalFace_staysInsideItsBounds() {
        for (use24Hour in listOf(true, false)) {
            val bounds = RectF(100f, 60f, 700f, 340f)
            val bitmap = render(800, 400, bounds, ClockFace.DIGITAL, ClockPalette.Light, at(second = 30), use24Hour)

            val inside = countInk(bitmap) { x, y -> bounds.contains(x, y) }
            val outside = countInk(bitmap) { x, y -> !bounds.contains(x, y) }
            assertTrue("nothing drawn (24h=$use24Hour)", inside > 0)
            assertEquals("ink outside bounds (24h=$use24Hour)", 0, outside)
        }
    }

    @Test
    fun emptyBounds_drawNothing() {
        for (face in ClockFace.entries) {
            val bitmap = render(100, 100, RectF(), face, ClockPalette.Dark, at(second = 30))
            assertEquals(0, countInk(bitmap) { _, _ -> true })
        }
    }

    private fun at(second: Int): LocalDateTime = LocalDateTime.of(2026, 11, 17, 22, 10, second)

    private fun renderAnalog(time: LocalDateTime): Bitmap =
        render(
            width = 600,
            height = 400,
            bounds = RectF(0f, 0f, 600f, 400f),
            face = ClockFace.ANALOG,
            palette = ClockPalette.Dark,
            time = time,
        )

    private fun render(
        width: Int,
        height: Int,
        bounds: RectF,
        face: ClockFace,
        palette: ClockPalette,
        time: LocalDateTime,
        use24Hour: Boolean = true,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        renderer.palette = palette
        renderer.drawBackground(canvas)
        renderer.draw(canvas, bounds, time, face, use24Hour)
        return bitmap
    }

    private fun pixelOnDial(bitmap: Bitmap, degrees: Float, distance: Float): Int {
        val angle = Math.toRadians(degrees.toDouble())
        val x = DIAL_CX + DIAL_RADIUS * distance * sin(angle)
        val y = DIAL_CY - DIAL_RADIUS * distance * cos(angle)
        return bitmap.getPixel(x.roundToInt(), y.roundToInt())
    }

    // Counts the pixels that aren't background color and whose center passes the filter.
    private fun countInk(bitmap: Bitmap, where: (x: Float, y: Float) -> Boolean): Int {
        val background = renderer.palette.background
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (bitmap.getPixel(x, y) != background && where(x + 0.5f, y + 0.5f)) count++
            }
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
