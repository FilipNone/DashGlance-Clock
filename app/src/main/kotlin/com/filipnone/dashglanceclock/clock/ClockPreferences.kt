package com.filipnone.dashglanceclock.clock

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

class ClockPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var face: ClockFace
        get() = ClockFace.fromName(prefs.getString(KEY_FACE, null))
        set(value) = prefs.edit { putString(KEY_FACE, value.name) }

    // SharedPreferences keeps only weak references to its listeners. The lifecycle observer
    // holds a strong one until onDestroy.
    fun observeFace(lifecycle: Lifecycle, onChange: (ClockFace) -> Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_FACE) onChange(face)
        }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                prefs.registerOnSharedPreferenceChangeListener(listener)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        })
    }

    private companion object {
        const val FILE_NAME = "clock"
        const val KEY_FACE = "face"
    }
}
