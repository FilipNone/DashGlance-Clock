package com.filipnone.dashglanceclock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.filipnone.dashglanceclock.media.MediaControlListener
import com.filipnone.dashglanceclock.media.MusicControls
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

    init {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun previewShowsTheTimeAndTheDate_likeTheCard() {
        val time = activity.findViewById<TextClock>(R.id.time)
        val date = activity.findViewById<TextClock>(R.id.date)

        assertEquals("h:mm", time.format12Hour.toString())
        assertEquals("HH:mm", time.format24Hour.toString())
        assertEquals("EEEE d MMMM", date.format12Hour.toString())
        assertEquals("EEEE d MMMM", date.format24Hour.toString())
        assertTrue(time.isShown)
        assertTrue(date.isShown)
    }

    @Test
    fun hintExplainsTheCardAndTheSwitch() {
        val hint = activity.findViewById<TextView>(R.id.hint).text.toString()

        assertTrue(hint, "music card" in hint)
        assertTrue(hint, "music controls" in hint)
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
    fun tappingTheCredit_opensTheCreatorsGitHubPage() {
        credit.performClick()

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://github.com/FilipNone", intent.dataString)
    }

    @Test
    @Config(qualifiers = "en-w800dp-h300dp-land")
    fun inShortLandscapeWindows_theSwitchScrollsIntoView_andTheCreditStaysAtTheBottom() {
        val scroll = activity.findViewById<ScrollView>(R.id.scroll)
        assertTrue(scroll.canScrollVertically(1))
        assertTrue(top(credit) >= bottom(scroll))
        assertTrue(bottom(credit) <= activity.window.decorView.height)

        scroll.fullScroll(View.FOCUS_DOWN)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(bottom(musicControls) <= bottom(scroll))
    }

    private fun grantNotificationAccess() {
        val listener = ComponentName(activity, MediaControlListener::class.java).flattenToString()
        Settings.Secure.putString(activity.contentResolver, "enabled_notification_listeners", listener)
    }

    private fun top(view: View): Int = IntArray(2).also { view.getLocationInWindow(it) }[1]

    private fun bottom(view: View): Int = top(view) + view.height

    private companion object {
        val NOTIFICATION_ACCESS_SCREENS = setOf(
            Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS,
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
        )
    }
}
