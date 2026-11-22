package com.filipnone.dashglanceclock.car

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.testing.ScreenController
import androidx.car.app.testing.SessionController
import androidx.car.app.testing.TestAppManager
import androidx.car.app.testing.TestCarContext
import androidx.car.app.testing.TestScreenManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.ClockPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ClockSessionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val carContext = TestCarContext.createCarContext(context)
    private val preferences = ClockPreferences(context)
    private val appManager = carContext.getCarService(TestAppManager::class.java)

    private lateinit var screen: ScreenController

    @Before
    fun startSession() {
        preferences.face = ClockFace.ANALOG
        SessionController(ClockSession(), carContext, Intent()).moveToState(Lifecycle.State.RESUMED)
        // SessionController pushes the first screen but doesn't start it. ScreenController
        // does, so the screen can send templates to the fake host.
        val top = carContext.getCarService(TestScreenManager::class.java).top
        screen = ScreenController(top).moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun opensTheClockScreen() {
        assertEquals(ClockScreen::class.java, screen.screen.javaClass)
    }

    @Test
    fun asksTheHostForTheDrawingSurface() {
        assertNotNull(appManager.surfaceCallback)
    }

    @Test
    fun faceChangedOnThePhone_refreshesTheCarScreen() {
        preferences.face = ClockFace.DIGITAL
        shadowOf(Looper.getMainLooper()).idle()

        val template = screen.templatesReturned.last() as NavigationTemplate
        assertEquals("Analog", template.actionStrip!!.actions.single().title.toString())
    }
}
