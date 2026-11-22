package com.filipnone.dashglanceclock.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

// The design sheet's drag handle: a short rounded line that can bend into a ^. The view is
// taller than the line so the ^ fits.
class SheetHandle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = LINE_WIDTH_DP * resources.displayMetrics.density
    }

    var color: Int
        get() = paint.color
        set(value) {
            paint.color = value
            invalidate()
        }

    // 0 draws the flat line, 1 the full ^, and values in between bend it part of the way.
    var chevron: Float = 0f
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    // The round caps stick out half a stroke past each end, so the flat line fills the view's
    // width exactly. Both halves turn around the middle; the tip rises as far as the ends drop,
    // which keeps the ^ centered on the line it came from.
    override fun onDraw(canvas: Canvas) {
        val half = (width - paint.strokeWidth) / 2f
        val angle = Math.toRadians(CHEVRON_ANGLE_DEGREES * chevron.toDouble())
        val dx = half * cos(angle).toFloat()
        val dy = half * sin(angle).toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        path.rewind()
        path.moveTo(centerX - dx, centerY + dy / 2f)
        path.lineTo(centerX, centerY - dy / 2f)
        path.lineTo(centerX + dx, centerY + dy / 2f)
        canvas.drawPath(path, paint)
    }

    companion object {
        // For ObjectAnimator. A property name string would find the setter by reflection,
        // which R8 can break in release builds.
        val CHEVRON = object : FloatProperty<SheetHandle>("chevron") {
            override fun setValue(handle: SheetHandle, value: Float) {
                handle.chevron = value
            }

            override fun get(handle: SheetHandle): Float = handle.chevron
        }

        private const val LINE_WIDTH_DP = 4f
        private const val CHEVRON_ANGLE_DEGREES = 35.0
    }
}
