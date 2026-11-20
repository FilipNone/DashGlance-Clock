package com.filipnone.dashglanceclock.media

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit

// Notification access is what lets DashGlance read and control other apps' music, and only
// the user can grant or revoke it, in Android's settings. The in-app switch is a second
// setting on top of it. Turning it off stops DashGlance from using the access without a trip
// to the settings, but the access itself stays granted until the user revokes it there.
class MusicControls(context: Context) {

    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    val hasAccess: Boolean
        get() = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    // On until the user turns the switch off, so granting access is enough to start.
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) {
            prefs.edit { putBoolean(KEY_ENABLED, value) }
            // The listener unbinds itself when controls go off (see MediaControlListener), and
            // stays unbound until it's asked back.
            if (value && hasAccess) {
                NotificationListenerService.requestRebind(ComponentName(context, MediaControlListener::class.java))
            }
        }

    val isOn: Boolean
        get() = isEnabled && hasAccess

    // Calls onChange whenever the switch is flipped, until stopObserving. SharedPreferences
    // only keeps a weak reference to its listeners, so this object holds the strong one.
    fun observe(onChange: () -> Unit) {
        stopObserving()
        listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == KEY_ENABLED) onChange()
        }.also(prefs::registerOnSharedPreferenceChangeListener)
    }

    fun stopObserving() {
        listener?.let(prefs::unregisterOnSharedPreferenceChangeListener)
        listener = null
    }

    private companion object {
        const val FILE_NAME = "music_controls"
        const val KEY_ENABLED = "enabled"
    }
}
