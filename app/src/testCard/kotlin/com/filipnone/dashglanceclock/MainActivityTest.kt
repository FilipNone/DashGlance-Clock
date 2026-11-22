package com.filipnone.dashglanceclock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.DigitalDesign
import com.filipnone.dashglanceclock.media.CardDesigns
import com.filipnone.dashglanceclock.media.MediaControlListener
import com.filipnone.dashglanceclock.media.MusicControls
import com.filipnone.dashglanceclock.ui.CardPreview
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
    private val musicControls = activity.findViewById<CompoundButton>(R.id.media_access)
    private val preview = activity.findViewById<CardPreview>(R.id.preview)
    private val designTitle = activity.findViewById<TextView>(R.id.design_title)
    private val stacked = activity.findViewById<CompoundButton>(R.id.stacked)
    private val coordinator = activity.findViewById<View>(R.id.coordinator)
    private val sheet = activity.findViewById<View>(R.id.design_panel)
    private val sheetHeader = activity.findViewById<View>(R.id.design_sheet_header)
    private val preferences = CardDesigns.preferences(activity)

    init {
        idle()
    }

    @Test
    fun previewShowsTheDigitalDashGlanceClock_atFirst() {
        assertTrue(preview.isShown)
        assertEquals("dashglance_digital", preview.design.id)
        assertEquals("Digital designs", designTitle.text.toString())
    }

    @Test
    fun openingTheApp_savesNothing() {
        assertTrue(activity.getSharedPreferences("card", Context.MODE_PRIVATE).all.isEmpty())
    }

    @Test
    fun hintExplainsTheCardAndTheTap() {
        val hint = activity.findViewById<TextView>(R.id.hint).text.toString()

        assertTrue(hint, "music card" in hint)
        assertTrue(hint, "Tap the clock" in hint)
    }

    @Test
    fun picker_listsOnlyTheDesignsTheCardCanShow() {
        assertTrue(tile("DashGlance")!!.isSelected)
        assertNotNull(tile("Toyota 1980s"))
        assertNotNull(tile("Flip clock"))
        assertNotNull(tile("Lagonda 1976"))
        assertNull(tile("Cyberpunk 2077"))
        assertNull(tile("Tron Legacy"))
        assertNull(tile("Cadillac 1980s"))
        assertNull(tile("Material You"))
    }

    @Test
    fun picker_showsTheDesignsInTheirDarkColors_likeTheCard() {
        assertTrue(tilePreview("Tesla").alwaysDark)
    }

    @Test
    fun tappingThePreview_switchesToAnalogDesigns_andHidesTheStackedSwitch() {
        preview.performClick()
        idle()

        assertEquals(ClockFace.ANALOG, preferences.face)
        assertEquals("dashglance_analog", preview.design.id)
        assertEquals("Analog designs", designTitle.text.toString())
        assertNotNull(tile("Jaguar 1960s"))
        assertNull(tile("Chevrolet 1950s"))
        assertFalse(stacked.isShown)
    }

    @Test
    fun tappingADesign_savesItAndShowsIt() {
        tile("Toyota 1980s")!!.performClick()
        idle()

        assertEquals("toyota_1980s", preferences.designFor(ClockFace.DIGITAL).id)
        assertEquals("toyota_1980s", preview.design.id)
        assertTrue(tile("Toyota 1980s")!!.isSelected)
    }

    @Test
    fun stackedSwitch_stacksThePreviewAndTheDesignsInTheList() {
        assertTrue(stacked.isShown)
        assertFalse(stacked.isChecked)
        assertFalse((preview.design as DigitalDesign).stacked)

        stacked.performClick()
        idle()

        assertTrue(preferences.stacked)
        assertTrue((preview.design as DigitalDesign).stacked)
        assertTrue((tilePreview("Tesla").design as DigitalDesign).stacked)
    }

    @Test
    fun aSavedDesignTheCardCantShow_fallsBackToDashGlance() {
        activity.getSharedPreferences("card", Context.MODE_PRIVATE).edit().putString("digital_design", "tron_legacy").commit()
        idle()

        assertEquals("dashglance_digital", preview.design.id)
        assertTrue(tile("DashGlance")!!.isSelected)
    }

    @Test
    fun designsSitInABottomSheet_underThePreview() {
        val behavior = BottomSheetBehavior.from(sheet)

        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, behavior.state)
        assertTrue(bottom(tile("DashGlance")!!) <= bottom(coordinator))
        assertTrue(bottom(preview) <= top(sheet))
        assertTrue(bottom(musicControls) <= top(sheet))

        sheetHeader.performClick()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))

        assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
        assertTrue(preview.coveredBottom > 0)
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
        assertEquals("DashGlance Card by FilipNone", credit.text.toString())
        assertNotNull(credit.compoundDrawablesRelative[0])
        assertEquals("DashGlance Card by FilipNone, on GitHub", credit.contentDescription)
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

    private fun grantNotificationAccess() {
        val listener = ComponentName(activity, MediaControlListener::class.java).flattenToString()
        Settings.Secure.putString(activity.contentResolver, "enabled_notification_listeners", listener)
    }

    private fun tile(label: String): View? {
        val matches = ArrayList<View>()
        activity.window.decorView.findViewsWithText(matches, label, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        return matches.singleOrNull { it.contentDescription == label }
    }

    private fun tilePreview(label: String): ClockView = (tile(label) as ViewGroup).getChildAt(0) as ClockView

    private fun top(view: View): Int = IntArray(2).also { view.getLocationInWindow(it) }[1]

    private fun bottom(view: View): Int = top(view) + view.height

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private companion object {
        val NOTIFICATION_ACCESS_SCREENS = setOf(
            Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS,
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
        )
    }
}
