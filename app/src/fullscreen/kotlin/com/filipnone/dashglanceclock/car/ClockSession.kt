package com.filipnone.dashglanceclock.car

import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.Screen
import androidx.car.app.Session
import com.filipnone.dashglanceclock.clock.ClockPreferences

class ClockSession : Session() {

    private var surfaceRenderer: ClockSurfaceRenderer? = null

    override fun onCreateScreen(intent: Intent): Screen {
        val preferences = ClockPreferences(carContext)
        val renderer = ClockSurfaceRenderer(carContext, lifecycle, preferences)
        val screen = ClockScreen(carContext, preferences)
        surfaceRenderer = renderer

        preferences.observe(lifecycle) {
            screen.invalidate()
            renderer.render()
        }
        return screen
    }

    // Called when the car switches between day and night mode. Redrawing here changes the
    // palette without waiting for the next tick.
    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        surfaceRenderer?.render()
    }
}
