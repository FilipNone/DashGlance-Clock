package com.filipnone.dashglanceclock.clock

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClockPreferencesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences = ClockPreferences(context)

    @Test
    fun startsWithTheDashGlanceDesigns() {
        assertSame(ClockDesigns.DashGlanceAnalog, preferences.designFor(ClockFace.ANALOG))
        assertSame(ClockDesigns.DashGlanceDigital, preferences.designFor(ClockFace.DIGITAL))
    }

    @Test
    fun keepsOneDesignPerFace_andShowsTheOneForTheCurrentFace() {
        preferences.select(design("rolls_royce_1930s"))
        preferences.select(design("tesla"))

        preferences.face = ClockFace.ANALOG
        assertEquals("rolls_royce_1930s", preferences.design.id)
        preferences.face = ClockFace.DIGITAL
        assertEquals("tesla", preferences.design.id)
    }

    @Test
    fun designsRemovedInALaterVersion_fallBackToTheDefault() {
        context.getSharedPreferences("clock", Context.MODE_PRIVATE).edit {
            putString("analog_design", "removed_design")
        }

        assertSame(ClockDesigns.DashGlanceAnalog, preferences.designFor(ClockFace.ANALOG))
    }

    private fun design(id: String): ClockDesign = ClockDesigns.all.single { it.id == id }
}
