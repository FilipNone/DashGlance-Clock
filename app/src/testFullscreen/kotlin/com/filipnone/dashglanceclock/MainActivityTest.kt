package com.filipnone.dashglanceclock

import android.content.Intent
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.ClockPreferences
import com.filipnone.dashglanceclock.ui.ClockView
import com.filipnone.dashglanceclock.ui.SheetHandle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-w400dp-h860dp")
class MainActivityTest {

    private val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    private val activity = controller.get()
    private val credit = activity.findViewById<TextView>(R.id.credit)
    private val clock = activity.findViewById<ClockView>(R.id.clock)
    private val designTitle = activity.findViewById<TextView>(R.id.design_title)
    private val coordinator = activity.findViewById<View>(R.id.coordinator)
    private val sheet = activity.findViewById<View>(R.id.design_panel)
    private val sheetHeader = activity.findViewById<View>(R.id.design_sheet_header)
    private val sheetHandle = activity.findViewById<SheetHandle>(R.id.design_sheet_handle)
    private val behavior = BottomSheetBehavior.from(sheet)
    private val preferences = ClockPreferences(activity)

    init {
        idle()
    }

    @Test
    fun hintOnlyExplainsTheTap() {
        val hint = activity.findViewById<TextView>(R.id.hint)

        assertEquals("Tap the clock to switch between analog and digital", hint.text.toString())
    }





    @Test
    fun creditNamesTheAppAndItsCreator_nextToTheGitHubMark() {
        assertEquals("DashGlance Clock by FilipNone", credit.text.toString())
        assertNotNull(credit.compoundDrawablesRelative[0])
        assertEquals("DashGlance Clock by FilipNone, on GitHub", credit.contentDescription)
    }

    @Test
    fun creditStaysAtTheBottom_underTheSheet() {
        assertTrue(top(credit) >= bottom(coordinator))
    }

    @Test
    fun tappingTheCredit_opensTheCreatorsGitHubPage() {
        credit.performClick()

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://github.com/FilipNone", intent.dataString)
    }

    @Test
    fun picker_listsTheDesignsForTheCurrentFace() {
        assertEquals("Analog designs", designTitle.text.toString())
        assertFalse(tile("Rolls-Royce 1930s")!!.isSelected)
        assertTrue(tile("DashGlance")!!.isSelected)
        assertNull(tile("Tesla"))
    }

    @Test
    fun picker_givesEachGroupItsOwnRow() {
        val original = top(tile("DashGlance")!!)
        val phones = top(tile("Material You")!!)
        val vintage = top(tile("Jaguar 1960s")!!)

        assertEquals(phones, top(tile("Apple iOS")!!))
        assertEquals(phones, top(tile("Huawei HarmonyOS")!!))
        assertTrue(original < phones)
        assertTrue(phones < vintage)
    }

    @Test
    fun collapsedSheet_showsTheFirstGroupOnly() {
        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, behavior.state)
        assertTrue(bottom(tile("DashGlance")!!) <= bottom(coordinator))
        assertTrue(top(tile("Material You")!!) >= bottom(coordinator))
        assertTrue(bottom(clock) <= top(sheet))
    }

    @Test
    @Config(qualifiers = "en-w320dp-h360dp-port")
    fun inShortWindows_theCollapsedSheetShowsOnlyItsHeader() {
        assertEquals(top(sheet) + sheetHeader.height, bottom(coordinator))
        assertTrue(bottom(clock) <= top(sheet))
    }

    @Test
    fun tappingTheSheetHeader_revealsTheOtherGroups_andShrinksTheClock() {
        assertEquals("Show all designs", clickLabel(sheetHeader))

        sheetHeader.performClick()
        settle()

        assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
        assertTrue(bottom(tile("Material You")!!) <= bottom(coordinator))
        assertTrue(clock.coveredBottom > 0)
        assertTrue(top(clock) + clock.height - clock.coveredBottom < top(sheet))
        assertEquals("Show fewer designs", clickLabel(sheetHeader))
    }

    @Test
    fun backCollapsesTheSheet_beforeLeavingTheApp() {
        sheetHeader.performClick()
        settle()

        activity.onBackPressedDispatcher.onBackPressed()
        settle()

        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, behavior.state)
        assertFalse(activity.isFinishing)
        assertEquals(0, clock.coveredBottom)
    }

    @Test
    fun aSecondAfterOpening_theSheetHopsToPeekAtMoreDesigns_withAChevronForAHandle() {
        runFramesOnTheClock()
        idleFor(900)
        assertEquals(0f, sheet.translationY)
        assertEquals(0f, sheetHandle.chevron)

        idleFor(100 + HOP_TOP_MS)
        assertTrue(sheet.translationY < 0f)
        assertTrue(sheetHandle.chevron > 0.5f)
        assertTrue(top(text("Phone brands")) < bottom(coordinator))

        idleFor(HOP_END_MS)
        assertEquals(0f, sheet.translationY)
        assertEquals(0f, sheetHandle.chevron)
        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, behavior.state)
    }

    @Test
    fun atTheTopOfTheHop_theSheetHoldsStill_asAFullChevron() {
        val samples = sampleTheFirstHop()

        val highest = samples.minOf { it.y }
        val atTheTop = samples.filter { it.y == highest }
        assertTrue(highest < 0f)
        assertTrue(atTheTop.last().time - atTheTop.first().time >= 30)
        assertTrue(atTheTop.all { it.chevron == 1f })
    }

    @Test
    fun afterTheHold_theSheetDropsAndBouncesLower_withAFlatHandle() {
        val samples = sampleTheFirstHop()

        val highest = samples.minOf { it.y }
        val afterTheTop = samples.drop(samples.indexOfLast { it.y == highest })
        val bounce = afterTheTop.drop((1 until afterTheTop.size).first { afterTheTop[it].y < afterTheTop[it - 1].y })
        assertTrue(bounce.minOf { it.y } < 0f)
        assertTrue(bounce.minOf { it.y } > highest / 4)
        assertTrue(bounce.all { it.chevron == 0f })
        assertEquals(0f, samples.last().y)
    }

    @Test
    fun aSecondAfterSwitchingTheClock_theSheetHopsAgain() {
        runFramesOnTheClock()
        idleFor(HINT_OVER_MS)

        clock.performClick()
        idleFor(900)
        assertEquals(0f, sheet.translationY)

        idleFor(100 + HOP_TOP_MS)
        assertTrue(sheet.translationY < 0f)
        assertTrue(sheetHandle.chevron > 0.5f)
    }

    @Test
    fun pickingADesign_doesNotMakeTheSheetHop() {
        runFramesOnTheClock()
        idleFor(HINT_OVER_MS)

        tile("Nothing OS")!!.performClick()
        idleFor(1000 + HOP_TOP_MS)

        assertEquals(0f, sheet.translationY)
    }

    @Test
    fun anOpenSheet_doesNotHop() {
        runFramesOnTheClock()
        sheetHeader.performClick()
        idleFor(1000 + HOP_TOP_MS)

        assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
        assertEquals(0f, sheet.translationY)
        assertEquals(0f, sheetHandle.chevron)
    }

    @Test
    fun openingTheSheetMidHop_stopsTheHop() {
        runFramesOnTheClock()
        idleFor(1000 + HOP_TOP_MS)
        assertTrue(sheet.translationY < 0f)

        sheetHeader.performClick()

        assertEquals(0f, sheet.translationY)
        assertEquals(0f, sheetHandle.chevron)
        settle()
        assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
    }

    @Test
    fun leavingTheApp_cancelsThePendingHop_andComingBackHopsAgain() {
        runFramesOnTheClock()
        controller.pause().stop()
        idleFor(1000 + HOP_TOP_MS)
        assertEquals(0f, sheet.translationY)

        controller.start().resume()
        idleFor(1000 + HOP_TOP_MS)
        assertTrue(sheet.translationY < 0f)
    }

    @Test
    fun tappingADesign_savesItAndShowsIt() {
        tile("Nothing OS")!!.performClick()
        idle()

        assertEquals("nothing_analog", preferences.designFor(ClockFace.ANALOG).id)
        assertEquals("nothing_analog", clock.design.id)
        assertTrue(tile("Nothing OS")!!.isSelected)
    }

    @Test
    fun tappingTheClock_switchesThePickerToDigitalDesigns() {
        clock.performClick()
        idle()

        assertEquals("Digital designs", designTitle.text.toString())
        assertEquals("dashglance_digital", clock.design.id)
        assertNotNull(tile("Tesla"))
        assertNull(tile("Rolls-Royce 1930s"))
    }

    private fun tile(label: String): View? {
        val matches = ArrayList<View>()
        activity.window.decorView.findViewsWithText(matches, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        return matches.singleOrNull { it.contentDescription == label }
    }

    private fun text(text: String): View {
        val matches = ArrayList<View>()
        activity.window.decorView.findViewsWithText(matches, text, View.FIND_VIEWS_WITH_TEXT)
        return matches.single()
    }

    private fun clickLabel(view: View): String? =
        AccessibilityNodeInfoCompat.wrap(view.createAccessibilityNodeInfo()).actionList
            .single { it.id == AccessibilityNodeInfoCompat.ACTION_CLICK }
            .label?.toString()

    private fun top(view: View): Int = IntArray(2).also { view.getLocationInWindow(it) }[1]

    private fun bottom(view: View): Int = top(view) + view.height

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun settle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    // By default Robolectric moves the clock forward on every frame, so a running animation
    // plays to its end inside a single idleFor. Paused, frames only come as the test moves the
    // clock, and the hop can be caught halfway.
    private fun runFramesOnTheClock() = ShadowChoreographer.setPaused(true)

    private fun sampleTheFirstHop(): List<HopSample> {
        runFramesOnTheClock()
        return List(HINT_OVER_MS.toInt() / 8) {
            idleFor(8)
            HopSample(SystemClock.uptimeMillis(), sheet.translationY, sheetHandle.chevron)
        }
    }

    private data class HopSample(val time: Long, val y: Float, val chevron: Float)

    private companion object {
        // The first hop starts a second after the app opens and takes 1.25s. HOP_TOP_MS into
        // it the sheet is near the top, and HOP_END_MS later it has stopped bouncing.
        // HINT_OVER_MS after opening, the first hint has played out.
        const val HOP_TOP_MS = 300L
        const val HOP_END_MS = 1400L
        const val HINT_OVER_MS = 3000L
    }
}
