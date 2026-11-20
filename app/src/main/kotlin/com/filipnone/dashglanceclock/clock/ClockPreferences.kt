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

    // Each face keeps its own design. The car can switch faces, and gets back whichever design
    // was picked for that face on the phone.
    val design: ClockDesign
        get() = designFor(face)

    fun designFor(face: ClockFace): ClockDesign =
        ClockDesigns.find(face, prefs.getString(designKey(face), null))

    fun select(design: ClockDesign) = prefs.edit { putString(designKey(design.face), design.id) }

    // SharedPreferences keeps only weak references to its listeners. The lifecycle observer
    // holds a strong one until onDestroy.
    fun observe(lifecycle: Lifecycle, onChange: () -> Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key in KEYS) onChange()
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

    private fun designKey(face: ClockFace): String = when (face) {
        ClockFace.ANALOG -> KEY_ANALOG_DESIGN
        ClockFace.DIGITAL -> KEY_DIGITAL_DESIGN
    }

    private companion object {
        const val FILE_NAME = "clock"
        const val KEY_FACE = "face"
        const val KEY_ANALOG_DESIGN = "analog_design"
        const val KEY_DIGITAL_DESIGN = "digital_design"
        val KEYS = setOf(KEY_FACE, KEY_ANALOG_DESIGN, KEY_DIGITAL_DESIGN)
    }
}
