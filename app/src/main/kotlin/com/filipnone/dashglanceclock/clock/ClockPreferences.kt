package com.filipnone.dashglanceclock.clock

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

// DashGlance Card keeps its choices in a file of its own, so the designs picked for the
// full-screen clock in 0.2, when both were one app, don't carry over to the card.
class ClockPreferences(
    context: Context,
    fileName: String = FILE_NAME,
    private val defaultFace: ClockFace = ClockFace.ANALOG,
) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    var face: ClockFace
        get() = ClockFace.fromName(prefs.getString(KEY_FACE, null), defaultFace)
        set(value) = prefs.edit { putString(KEY_FACE, value.name) }

    // Each face keeps its own design. The car can switch faces, and gets back whichever design
    // was picked for that face on the phone.
    val design: ClockDesign
        get() = designFor(face)

    fun designFor(face: ClockFace): ClockDesign =
        ClockDesigns.find(face, prefs.getString(designKey(face), null))

    fun select(design: ClockDesign) = prefs.edit { putString(designKey(design.face), design.id) }

    // Hours above minutes on the digital face. Only DashGlance Card offers it, for its narrow card.
    var stacked: Boolean
        get() = prefs.getBoolean(KEY_STACKED, false)
        set(value) = prefs.edit { putBoolean(KEY_STACKED, value) }

    // SharedPreferences keeps only weak references to its listeners. The lifecycle observer
    // holds a strong one until onDestroy.
    fun observe(lifecycle: Lifecycle, onChange: () -> Unit) {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            private var stop: (() -> Unit)? = null

            override fun onCreate(owner: LifecycleOwner) {
                stop = listen(onChange)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                stop?.invoke()
                stop = null
            }
        })
    }

    // For callers without a lifecycle, such as a service. The returned function stops the
    // calls, and holding it keeps the listener alive until then.
    fun listen(onChange: () -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key in KEYS) onChange()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
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
        const val KEY_STACKED = "stacked"
        val KEYS = setOf(KEY_FACE, KEY_ANALOG_DESIGN, KEY_DIGITAL_DESIGN, KEY_STACKED)
    }
}
