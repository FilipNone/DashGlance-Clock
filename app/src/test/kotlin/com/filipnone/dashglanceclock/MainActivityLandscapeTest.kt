package com.filipnone.dashglanceclock

import android.os.Looper
import android.view.View
import android.widget.TextView
import com.filipnone.dashglanceclock.clock.ClockDesigns
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.ClockPreferences
import com.filipnone.dashglanceclock.ui.ClockView
import com.filipnone.dashglanceclock.ui.DesignPicker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-w860dp-h400dp-land")
class MainActivityLandscapeTest {

    private val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
    private val clock = activity.findViewById<ClockView>(R.id.clock)
    private val panel = activity.findViewById<View>(R.id.design_panel)
    private val designs = activity.findViewById<DesignPicker>(R.id.designs)
    private val credit = activity.findViewById<TextView>(R.id.credit)

    init {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun clockAndDesignsSplitTheScreenInHalf() {
        assertTrue(abs(clock.width - panel.width) <= 1)
        assertTrue(right(clock) < left(panel))
        assertEquals(top(clock), top(panel))
        assertEquals(0, clock.coveredBottom)
    }

    @Test
    fun designsSitInASidePanel_notASheet() {
        assertNull(activity.findViewById<View>(R.id.coordinator))
        assertNull(activity.findViewById<View>(R.id.design_sheet_header))
    }

    @Test
    fun sidePanelShowsSeveralGroups_andScrollsToTheRest() {
        assertTrue(bottom(tile("DashGlance")!!) < top(tile("Material You")!!))
        assertTrue(top(tile("Material You")!!) < bottom(panel))
        assertTrue(designs.canScrollVertically(1))
    }

    @Test
    fun sidePanelOpensAtTheSelectedDesign() {
        val preferences = ClockPreferences(activity)
        preferences.select(ClockDesigns.all.single { it.id == "rolls_royce_1930s" })
        val reopened = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        shadowOf(Looper.getMainLooper()).idle()

        val selected = tile("Rolls-Royce 1930s", reopened)!!
        val reopenedPanel = reopened.findViewById<View>(R.id.design_panel)
        assertTrue(selected.isSelected)
        assertTrue(top(selected) >= top(reopenedPanel))
        assertTrue(bottom(selected) <= bottom(reopenedPanel))
        assertTrue(right(selected) <= right(reopenedPanel))
    }

    @Test
    fun musicControlsSwitchSitsUnderTheClock() {
        val musicControls = activity.findViewById<View>(R.id.media_access)

        assertTrue(musicControls.isShown)
        assertTrue(top(musicControls) >= bottom(clock))
        assertTrue(bottom(musicControls) <= top(credit))
    }

    @Test
    fun creditSitsUnderTheClock() {
        val creditCenter = left(credit) + credit.width / 2

        assertTrue(top(credit) >= bottom(clock))
        assertTrue(creditCenter in left(clock)..right(clock))
    }

    @Test
    fun tappingADesign_savesIt() {
        tile("Jaguar 1960s")!!.performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("jaguar_1960s", ClockPreferences(activity).designFor(ClockFace.ANALOG).id)
        assertEquals("jaguar_1960s", clock.design.id)
    }

    @Test
    fun backLeavesTheApp() {
        activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(activity.isFinishing)
    }

    private fun tile(label: String, activity: MainActivity = this.activity): View? {
        val matches = ArrayList<View>()
        activity.window.decorView.findViewsWithText(matches, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        return matches.singleOrNull { it.contentDescription == label }
    }

    private fun location(view: View): IntArray = IntArray(2).also { view.getLocationInWindow(it) }

    private fun left(view: View): Int = location(view)[0]

    private fun right(view: View): Int = left(view) + view.width

    private fun top(view: View): Int = location(view)[1]

    private fun bottom(view: View): Int = top(view) + view.height
}
