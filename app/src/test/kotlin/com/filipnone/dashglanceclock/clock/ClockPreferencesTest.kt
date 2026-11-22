package com.filipnone.dashglanceclock.clock

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun startsWithTheAnalogFace_unlessToldOtherwise() {
        assertEquals(ClockFace.ANALOG, preferences.face)
        assertEquals(ClockFace.DIGITAL, ClockPreferences(context, "other", defaultFace = ClockFace.DIGITAL).face)
    }

    @Test
    fun separateFiles_keepSeparateChoices() {
        val other = ClockPreferences(context, "other")
        preferences.select(design("tesla"))
        preferences.stacked = true

        assertSame(ClockDesigns.DashGlanceDigital, other.designFor(ClockFace.DIGITAL))
        assertFalse(other.stacked)
    }

    @Test
    fun listen_reportsChangesUntilStopped() {
        var changes = 0
        val stop = preferences.listen { changes++ }

        preferences.stacked = true
        preferences.face = ClockFace.DIGITAL
        stop()
        preferences.stacked = false

        assertEquals(2, changes)
        assertFalse(preferences.stacked)
    }

    private fun design(id: String): ClockDesign = ClockDesigns.all.single { it.id == id }
}
