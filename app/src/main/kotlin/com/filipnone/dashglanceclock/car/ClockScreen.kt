package com.filipnone.dashglanceclock.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import com.filipnone.dashglanceclock.R
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.ClockPreferences

class ClockScreen(
    carContext: CarContext,
    private val preferences: ClockPreferences,
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val nextFace = preferences.face.toggled()
        val switchFace = Action.Builder()
            .setTitle(carContext.getString(nextFace.labelRes()))
            .setOnClickListener { preferences.face = nextFace }
            .build()

        // The template holds nothing but the action strip, so ClockSurfaceRenderer gets the
        // rest of the screen.
        return NavigationTemplate.Builder()
            .setActionStrip(ActionStrip.Builder().addAction(switchFace).build())
            .build()
    }

    private fun ClockFace.labelRes(): Int = when (this) {
        ClockFace.ANALOG -> R.string.face_analog
        ClockFace.DIGITAL -> R.string.face_digital
    }
}
