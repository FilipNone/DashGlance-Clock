package com.filipnone.dashglanceclock

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Switch
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "notnight-xhdpi")
class PillSwitchTest {

    private val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_DashGlanceClock)
    private val density = context.resources.displayMetrics.density

    @Test
    fun isAPill_twoThumbsWide() {
        val toggle = measured(checked = false)

        assertEquals(px(52f), toggle.measuredWidth)
        assertEquals(px(30f), toggle.measuredHeight)
    }

    @Test
    fun off_hasAWhiteThumbOnTheLeft_onAPaleAccentTrack() {
        val bitmap = render(checked = false)

        assertEquals(Color.WHITE, pixel(bitmap, 15f, 15f))
        val track = pixel(bitmap, 37f, 15f)
        assertTrue("track alpha ${Color.alpha(track)}", Color.alpha(track) in 60..120)
        assertSameHue(ACCENT, track)
    }

    @Test
    fun on_hasAWhiteThumbOnTheRight_onTheFullAccent() {
        val bitmap = render(checked = true)

        assertEquals(Color.WHITE, pixel(bitmap, 37f, 15f))
        assertEquals(ACCENT, pixel(bitmap, 15f, 15f))
    }

    @Test
    fun thumbCastsAShadowBelowIt() {
        val bitmap = render(checked = true)

        val justBelowThumb = pixel(bitmap, 37f, 26.5f)
        val furtherDown = pixel(bitmap, 37f, 28.5f)
        assertTrue(ColorUtils.calculateLuminance(justBelowThumb) < ColorUtils.calculateLuminance(furtherDown))
    }

    @Test
    fun flippingTheSwitch_playsATransition_thenSettles() {
        val toggle = measured(checked = false)
        assertTrue(toggle.trackDrawable.current is VectorDrawable)

        toggle.isChecked = true
        assertTrue(toggle.trackDrawable.current is AnimatedVectorDrawable)

        toggle.jumpDrawablesToCurrentState()
        assertTrue(toggle.trackDrawable.current is VectorDrawable)
    }

    @Test
    fun turningOn_easesOut() {
        val progress = thumbProgressDuring(R.drawable.switch_turn_on, from = 15f, to = 37f)

        assertEaseOut(progress)
    }

    @Test
    fun turningOff_easesOut() {
        val progress = thumbProgressDuring(R.drawable.switch_turn_off, from = 37f, to = 15f)

        assertEaseOut(progress)
    }

    // On a phone the transition runs on the render thread, which Robolectric doesn't drive, so
    // this plays it on the main thread instead (a hidden method, fine in a test) with the
    // frame clock under the test's control. Returns how far the thumb has gone, from 0 to 1,
    // every 25ms.
    private fun thumbProgressDuring(transitionId: Int, from: Float, to: Float): List<Float> {
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(5))
        val transition = context.getDrawable(transitionId) as AnimatedVectorDrawable
        AnimatedVectorDrawable::class.java.getDeclaredMethod("forceAnimationOnUI")
            .apply { isAccessible = true }
            .invoke(transition)
        transition.setBounds(0, 0, px(52f), px(30f))
        transition.start()

        val looper = shadowOf(Looper.getMainLooper())
        return (0..12).map { step ->
            if (step > 0) looper.idleFor(Duration.ofMillis(25))
            val bitmap = Bitmap.createBitmap(px(52f), px(30f), Bitmap.Config.ARGB_8888)
            transition.draw(Canvas(bitmap))
            val white = (0 until bitmap.width).filter { bitmap.getPixel(it, px(15f)) == Color.WHITE }
            val center = (white.first() + white.last() + 1) / 2f / density
            (center - from) / (to - from)
        }
    }

    // Ease out covers most of the distance early and slows down at the end. With the plain
    // Switch's accelerate-decelerate easing, the thumb is under a quarter of the way at 75ms.
    private fun assertEaseOut(progress: List<Float>) {
        assertTrue("at 75ms: ${progress[3]}", progress[3] > 0.5f)
        assertTrue("the last half of the time: ${progress.drop(6)}", progress[12] - progress[6] < 0.25f)
        assertEquals(1f, progress.last(), 0.05f)
        assertEquals(progress, progress.sorted())
    }

    // Attaching a view to a window jumps its drawables to their current state, so a switch
    // that opens already on doesn't animate. A test view isn't attached, so it jumps by hand.
    private fun measured(checked: Boolean): Switch = Switch(context, null, 0, R.style.Widget_DashGlance_PillSwitch).apply {
        isChecked = checked
        jumpDrawablesToCurrentState()
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        measure(unspecified, unspecified)
        layout(0, 0, measuredWidth, measuredHeight)
    }

    private fun render(checked: Boolean): Bitmap {
        val toggle = measured(checked)
        val bitmap = Bitmap.createBitmap(toggle.width, toggle.height, Bitmap.Config.ARGB_8888)
        toggle.draw(Canvas(bitmap))
        return bitmap
    }

    private fun px(dp: Float): Int = (dp * density).roundToInt()

    private fun pixel(bitmap: Bitmap, xDp: Float, yDp: Float): Int =
        bitmap.getPixel((xDp * density).toInt(), (yDp * density).toInt())

    private fun assertSameHue(expected: Int, actual: Int) {
        val close = abs(Color.red(expected) - Color.red(actual)) <= 3 &&
            abs(Color.green(expected) - Color.green(actual)) <= 3 &&
            abs(Color.blue(expected) - Color.blue(actual)) <= 3
        assertTrue("expected the hue of ${Integer.toHexString(expected)}, got ${Integer.toHexString(actual)}", close)
    }

    private companion object {
        // The light theme's colorAccent.
        const val ACCENT = 0xFFD9480F.toInt()
    }
}
