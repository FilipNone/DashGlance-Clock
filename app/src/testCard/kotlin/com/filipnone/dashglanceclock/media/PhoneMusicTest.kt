package com.filipnone.dashglanceclock.media

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhoneMusicTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var cardUpdates = 0
    private val music: PhoneMusic

    init {
        val listener = ComponentName(context, MediaControlListener::class.java).flattenToString()
        Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners", listener)
        music = PhoneMusic(context) { cardUpdates++ }
    }

    @After
    fun release() = music.release()

    @Test
    fun withAccessAndControlsOn_theCardStopsAskingForAccess() {
        music.refresh()

        assertTrue(music.isAllowed)
        assertEquals(1, cardUpdates)
    }

    @Test
    fun turningControlsOffInTheApp_updatesTheCardAtOnce() {
        music.refresh()
        cardUpdates = 0

        MusicControls(context).isEnabled = false

        assertFalse(music.isAllowed)
        assertEquals(1, cardUpdates)
    }

    @Test
    fun turningControlsBackOn_updatesTheCardAtOnce() {
        MusicControls(context).isEnabled = false
        music.refresh()
        cardUpdates = 0

        MusicControls(context).isEnabled = true

        assertTrue(music.isAllowed)
        assertEquals(1, cardUpdates)
    }

    @Test
    fun afterRelease_theSwitchNoLongerReachesIt() {
        music.refresh()
        music.release()
        cardUpdates = 0

        MusicControls(context).isEnabled = false

        assertEquals(0, cardUpdates)
    }
}
