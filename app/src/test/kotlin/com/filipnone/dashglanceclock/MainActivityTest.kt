package com.filipnone.dashglanceclock

import android.content.Intent
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
    private val credit = activity.findViewById<TextView>(R.id.credit)

    @Test
    fun creditNamesTheAppAndItsCreator() {
        assertEquals("DashGlance Clock by FilipNone", credit.text.toString())
    }

    @Test
    fun tappingTheCredit_opensTheCreatorsGitHubPage() {
        credit.performClick()

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://github.com/FilipNone", intent.dataString)
    }
}
