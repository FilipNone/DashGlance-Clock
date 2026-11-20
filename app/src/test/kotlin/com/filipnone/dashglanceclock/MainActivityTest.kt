package com.filipnone.dashglanceclock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.core.app.ApplicationProvider
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.ClockPreferences
import com.filipnone.dashglanceclock.media.MediaControlListener
import com.filipnone.dashglanceclock.media.MusicControls
import com.filipnone.dashglanceclock.ui.ClockView
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
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-w400dp-h860dp")
class MainActivityTest {

    init {
        // NotificationManagerCompat keeps the last listener list in a static field and returns
        // it while the setting is missing, so access granted in an earlier test would leak into
        // this one. Revoking access on a phone leaves an empty list, which is what this sets.
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners", "")
    }

    private val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    private val activity = controller.get()
    private val credit = activity.findViewById<TextView>(R.id.credit)
    private val clock = activity.findViewById<ClockView>(R.id.clock)
    private val designTitle = activity.findViewById<TextView>(R.id.design_title)
    private val coordinator = activity.findViewById<View>(R.id.coordinator)
    private val sheet = activity.findViewById<View>(R.id.design_panel)
    private val sheetHeader = activity.findViewById<View>(R.id.design_sheet_header)
    private val behavior = BottomSheetBehavior.from(sheet)
    private val musicControls = activity.findViewById<CompoundButton>(R.id.media_access)
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
    fun musicControlsSwitch_isOff_withoutNotificationAccess() {
        assertEquals("Allow music controls", musicControls.text.toString())
        assertTrue(musicControls.isShown)
        assertFalse(musicControls.isChecked)
    }

    @Test
    fun musicControlsSwitch_isOn_onceAccessIsGranted() {
        grantNotificationAccess()
        controller.pause().resume()

        assertTrue(musicControls.isChecked)
    }

    @Test
    fun turningTheSwitchOn_withoutAccess_opensNotificationAccessSettings() {
        musicControls.performClick()

        val intent = shadowOf(activity).nextStartedActivity
        assertTrue(intent.action in NOTIFICATION_ACCESS_SCREENS)
    }

    @Test
    fun switchFlipsBack_whenTheUserReturnsWithoutGrantingAccess() {
        musicControls.performClick()
        controller.pause().resume()

        assertFalse(musicControls.isChecked)
    }

    @Test
    fun turningTheSwitchOff_staysInTheApp() {
        grantNotificationAccess()
        controller.pause().resume()

        musicControls.performClick()

        assertNull(shadowOf(activity).nextStartedActivity)
        assertFalse(musicControls.isChecked)
        assertFalse(MusicControls(activity).isOn)
        controller.pause().resume()
        assertFalse(musicControls.isChecked)
    }

    @Test
    fun turningTheSwitchBackOn_withAccessStillGranted_staysInTheApp() {
        grantNotificationAccess()
        MusicControls(activity).isEnabled = false
        controller.pause().resume()
        assertFalse(musicControls.isChecked)

        musicControls.performClick()

        assertNull(shadowOf(activity).nextStartedActivity)
        assertTrue(musicControls.isChecked)
        assertTrue(MusicControls(activity).isOn)
    }

    @Test
    fun syncingTheSwitchOnResume_doesNotOpenSettings() {
        grantNotificationAccess()
        controller.pause().resume()

        assertNull(shadowOf(activity).nextStartedActivity)
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

    private fun grantNotificationAccess() {
        val listener = ComponentName(activity, MediaControlListener::class.java).flattenToString()
        Settings.Secure.putString(activity.contentResolver, "enabled_notification_listeners", listener)
    }

    private fun clickLabel(view: View): String? =
        AccessibilityNodeInfoCompat.wrap(view.createAccessibilityNodeInfo()).actionList
            .single { it.id == AccessibilityNodeInfoCompat.ACTION_CLICK }
            .label?.toString()

    private fun top(view: View): Int = IntArray(2).also { view.getLocationInWindow(it) }[1]

    private fun bottom(view: View): Int = top(view) + view.height

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun settle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))

    private companion object {
        val NOTIFICATION_ACCESS_SCREENS = setOf(
            Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS,
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
        )
    }
}
