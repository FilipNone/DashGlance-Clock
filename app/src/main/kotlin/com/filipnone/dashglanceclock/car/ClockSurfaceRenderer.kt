package com.filipnone.dashglanceclock.car

import android.graphics.Rect
import android.graphics.RectF
import android.text.format.DateFormat
import android.util.Log
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.filipnone.dashglanceclock.clock.ClockPalette
import com.filipnone.dashglanceclock.clock.ClockPreferences
import com.filipnone.dashglanceclock.clock.ClockRenderer
import com.filipnone.dashglanceclock.clock.SecondTicker
import java.time.LocalDateTime
import kotlin.math.min

class ClockSurfaceRenderer(
    private val carContext: CarContext,
    lifecycle: Lifecycle,
    private val preferences: ClockPreferences,
) : DefaultLifecycleObserver {

    private val clockRenderer = ClockRenderer()
    private val ticker = SecondTicker { render() }
    private val clockBounds = RectF()

    private var surface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var visibleArea: Rect? = null
    private var stableArea: Rect? = null

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            surface = surfaceContainer.surface
            surfaceWidth = surfaceContainer.width
            surfaceHeight = surfaceContainer.height
            render()
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {
            this@ClockSurfaceRenderer.visibleArea = Rect(visibleArea)
            render()
        }

        override fun onStableAreaChanged(stableArea: Rect) {
            this@ClockSurfaceRenderer.stableArea = Rect(stableArea)
            render()
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            surface = null
        }
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
    }

    override fun onStart(owner: LifecycleOwner) {
        ticker.start()
    }

    override fun onStop(owner: LifecycleOwner) {
        ticker.stop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        ticker.stop()
        surface = null
    }

    fun render() {
        val target = surface?.takeIf { it.isValid } ?: return
        val canvas = try {
            target.lockCanvas(null)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not lock the car surface", e)
            return
        }
        try {
            clockRenderer.palette = ClockPalette.forDarkMode(carContext.isDarkMode)
            clockRenderer.drawBackground(canvas)
            updateClockBounds()
            clockRenderer.draw(
                canvas = canvas,
                bounds = clockBounds,
                now = LocalDateTime.now(),
                face = preferences.face,
                use24Hour = DateFormat.is24HourFormat(carContext),
            )
        } finally {
            target.unlockCanvasAndPost(canvas)
        }
    }

    // The visible area changes size as the action strip fades in and out, but the stable area
    // doesn't. Laying out against the stable area keeps the clock from jumping. If the host
    // hasn't reported either area yet, the clock uses the whole surface.
    private fun updateClockBounds() {
        val area = stableArea?.takeUnless { it.isEmpty } ?: visibleArea?.takeUnless { it.isEmpty }
        if (area != null) {
            clockBounds.set(area)
        } else {
            clockBounds.set(0f, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        }
        val margin = min(clockBounds.width(), clockBounds.height()) * EDGE_MARGIN_RATIO
        clockBounds.inset(margin, margin)
    }

    private companion object {
        const val TAG = "ClockSurfaceRenderer"
        const val EDGE_MARGIN_RATIO = 0.06f
    }
}
