package com.filipnone.dashglanceclock.media

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowNotificationListenerService

@RunWith(RobolectricTestRunner::class)
class MediaControlListenerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val controls = MusicControls(context)

    @Test
    fun staysConnected_whileMusicControlsAreOn() {
        grantAccess(true)
        val listener = connect()

        assertEquals(0, unbindRequests(listener))
    }

    @Test
    fun disconnectsAtOnce_whenMusicControlsAreAlreadyOff() {
        grantAccess(true)
        controls.isEnabled = false

        val listener = connect()

        assertEquals(1, unbindRequests(listener))
    }

    @Test
    fun disconnects_whenMusicControlsAreTurnedOff() {
        grantAccess(true)
        val listener = connect()

        controls.isEnabled = false

        assertEquals(1, unbindRequests(listener))
    }

    @Test
    fun turningMusicControlsOn_asksAndroidToReconnectIt() {
        grantAccess(true)
        controls.isEnabled = false
        val before = ShadowNotificationListenerService.getRebindRequestCount()

        controls.isEnabled = true

        assertEquals(before + 1, ShadowNotificationListenerService.getRebindRequestCount())
    }

    @Test
    fun withoutAccess_thereIsNothingToReconnect() {
        grantAccess(false)
        val before = ShadowNotificationListenerService.getRebindRequestCount()

        controls.isEnabled = true

        assertEquals(before, ShadowNotificationListenerService.getRebindRequestCount())
    }

    private fun connect(): MediaControlListener =
        Robolectric.buildService(MediaControlListener::class.java).create().get().apply { onListenerConnected() }

    private fun unbindRequests(listener: MediaControlListener): Int =
        Shadow.extract<ShadowNotificationListenerService>(listener).unbindRequestCount

    // NotificationManagerCompat caches the last listener list, so access is always written,
    // even when it's off.
    private fun grantAccess(granted: Boolean) {
        val listener = ComponentName(context, MediaControlListener::class.java).flattenToString()
        Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners", if (granted) listener else "")
    }
}
