package com.filipnone.dashglanceclock.car

import android.content.Context
import android.os.Looper
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.Action
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.testing.ScreenController
import androidx.car.app.testing.TestCarContext
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.ClockPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ClockScreenTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val carContext = TestCarContext.createCarContext(context)
    private val preferences = ClockPreferences(context)

    @Test
    fun analogFace_offersSwitchToDigital() {
        preferences.face = ClockFace.ANALOG

        assertEquals("Digital", showScreen().title.toString())
    }

    @Test
    fun digitalFace_offersSwitchToAnalog() {
        preferences.face = ClockFace.DIGITAL

        assertEquals("Analog", showScreen().title.toString())
    }

    @Test
    fun clickingTheAction_storesTheOtherFace() {
        preferences.face = ClockFace.ANALOG

        showScreen().onClickDelegate!!.sendClick(object : OnDoneCallback {})
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(ClockFace.DIGITAL, preferences.face)
    }

    // Puts the screen on a fake host and returns the switch-face action from its template.
    private fun showScreen(): Action {
        val controller = ScreenController(ClockScreen(carContext, preferences))
            .moveToState(Lifecycle.State.RESUMED)
        controller.screen.invalidate()
        val template = controller.templatesReturned.single() as NavigationTemplate
        return template.actionStrip!!.actions.single()
    }
}
