package com.filipnone.dashglanceclock.ui

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.TimeInterpolator
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.filipnone.dashglanceclock.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

// A view that draws itself only in the part above coveredBottom, the pixels at its bottom that
// something else covers. ClockView and DashGlance Card's preview are both one.
interface PartlyCovered {
    var coveredBottom: Int
}

// The bottom sheet that holds the design picker. Collapsed, it shows the first group of designs.
// Pulled up, it shows all of them and the clock above shrinks to stay in view.
class DesignSheet<Clock>(
    activity: ComponentActivity,
    private val sheet: View,
    private val header: View,
    private val handle: SheetHandle,
    private val picker: DesignPicker,
    private val content: View,
    private val clock: Clock,
) where Clock : View, Clock : PartlyCovered {

    private val behavior = BottomSheetBehavior.from(sheet)
    private val coordinator = sheet.parent as View
    private val lifecycle = activity.lifecycle
    private val density = sheet.resources.displayMetrics.density
    private val clockGap = (CLOCK_GAP_DP * density).roundToInt()
    private var shownState = NO_STATE
    private val startHop = Runnable { hop() }
    private var hopAnimation: Animator? = null

    // Back collapses the open sheet first, with the predictive back animation on Android 14
    // and newer. The callback is off while the sheet is collapsed, so back leaves the app then.
    private val back = object : OnBackPressedCallback(false) {
        override fun handleOnBackStarted(backEvent: BackEventCompat) = behavior.startBackProgress(backEvent)

        override fun handleOnBackProgressed(backEvent: BackEventCompat) = behavior.updateBackProgress(backEvent)

        override fun handleOnBackPressed() = behavior.handleBackInvoked()

        override fun handleOnBackCancelled() = behavior.cancelBackProgress()
    }

    init {
        activity.onBackPressedDispatcher.addCallback(activity, back)
        ViewCompat.setAccessibilityHeading(header, true)
        header.setOnClickListener { toggle() }
        sheet.viewTreeObserver.addOnPreDrawListener(ViewTreeObserver.OnPreDrawListener { sync() })
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = hint()

            override fun onStop(owner: LifecycleOwner) = stopHint()
        })
        // Once the user drags or taps the sheet, the hop has nothing left to show, and a
        // translation left over from it would add to the drag.
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState != BottomSheetBehavior.STATE_COLLAPSED) stopHint()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        })
    }

    // Shows that the sheet pulls up. A second from now, the collapsed sheet hops to peek at the
    // next group and its handle bends into a ^ for the jump. Runs each time the app comes to
    // the front, and MainActivity calls it after a face switch. Calling it again restarts the
    // second; calls while the app is in the background are dropped.
    fun hint() {
        sheet.removeCallbacks(startHop)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            sheet.postDelayed(startHop, HINT_DELAY_MS)
        }
    }

    private fun stopHint() {
        sheet.removeCallbacks(startHop)
        hopAnimation?.cancel()
        hopAnimation = null
        sheet.translationY = 0f
        handle.chevron = 0f
    }

    // The hop moves the sheet with translationY, which BottomSheetBehavior doesn't track, so the
    // behavior stays collapsed and sync() keeps the clock as it is. The lift is capped at the
    // part of the sheet below the screen, so its bottom edge never comes into view.
    private fun hop() {
        if (!sheet.isLaidOut || behavior.state != BottomSheetBehavior.STATE_COLLAPSED) return
        val lift = min(HOP_DP * density, (sheet.bottom - coordinator.height).toFloat())
        if (lift <= 0f) return
        val jump = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_Y, *jumpKeyframes(-lift))
        val morph = PropertyValuesHolder.ofKeyframe(SheetHandle.CHEVRON, *morphKeyframes())
        hopAnimation?.cancel()
        hopAnimation = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofPropertyValuesHolder(sheet, jump),
                ObjectAnimator.ofPropertyValuesHolder(handle, morph),
            )
            duration = HOP_MS.toLong()
            // The keyframes carry the easing. The default interpolator would also ease the
            // whole timeline, running the middle faster and cutting the hold short.
            interpolator = LinearInterpolator()
            start()
        }
    }

    // The sheet rises like a spring being pulled, slowing into the top, and holds there for
    // HOLD_MS. Then it drops like a dropped ball: it speeds up as it falls, and each bounce
    // leaves the ground at the speed it landed and slows to a stop on the way up.
    private fun jumpKeyframes(top: Float): Array<Keyframe> {
        val frames = arrayListOf(
            keyframe(0, 0f),
            keyframe(RISE_MS, top, PathInterpolator(0.2f, 0f, 0f, 1f)),
            keyframe(RISE_MS + HOLD_MS, top),
            keyframe(LANDING_MS, 0f, AccelerateInterpolator()),
        )
        var time = LANDING_MS
        BOUNCE_HEIGHTS.forEachIndexed { i, height ->
            frames += keyframe(time + BOUNCE_MS[i], top * height, DecelerateInterpolator())
            frames += keyframe(time + 2 * BOUNCE_MS[i], 0f, AccelerateInterpolator())
            time += 2 * BOUNCE_MS[i]
        }
        return frames.toTypedArray()
    }

    // The ^ forms on the way up and is complete at the top. It stays bent for most of the drop
    // and flattens just before the landing, so the bounces happen with a flat handle.
    private fun morphKeyframes(): Array<Keyframe> = arrayOf(
        keyframe(0, 0f),
        keyframe(RISE_MS, 1f, PathInterpolator(0.2f, 0f, 0f, 1f)),
        keyframe(RISE_MS + HOLD_MS, 1f),
        keyframe(LANDING_MS, 0f, AccelerateInterpolator(MORPH_SNAP)),
        keyframe(HOP_MS, 0f),
    )

    // A keyframe's easing applies to the stretch that ends at it, not the one that starts there.
    private fun keyframe(timeMs: Int, value: Float, easing: TimeInterpolator? = null): Keyframe =
        Keyframe.ofFloat(timeMs / HOP_MS.toFloat(), value).apply { interpolator = easing }

    private fun toggle() {
        behavior.state = if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            BottomSheetBehavior.STATE_EXPANDED
        } else {
            BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    // Runs before every frame, so it catches layout passes, drags and the settle animation alike.
    // The peek height depends on the picker's layout, which depends on the font size and the
    // face, so it's measured here instead of being set once. Returning false skips a frame
    // that would show the old sizes.
    private fun sync(): Boolean {
        if (!sheet.isLaidOut) return true

        // In a short window, such as split screen, the first group alone would cover most of
        // the clock, so the collapsed sheet shows only its header there. Landscape doesn't use
        // the sheet at all; it has its own layout with the designs beside the clock.
        val firstGroupPeek = picker.top + picker.firstGroupBottom
        val peek = if (firstGroupPeek <= coordinator.height * COLLAPSED_MAX_SHARE) firstGroupPeek else picker.top
        val maxHeight = max(peek, (coordinator.height * EXPANDED_MAX_SHARE).roundToInt())
        var relayout = false
        if (behavior.peekHeight != peek) {
            behavior.peekHeight = peek
            relayout = true
        }
        // The content ends where the collapsed sheet starts, so nothing sits behind it.
        if (content.paddingBottom != peek) {
            content.updatePadding(bottom = peek)
            relayout = true
        }
        if (behavior.maxHeight != maxHeight) {
            behavior.maxHeight = maxHeight
            sheet.requestLayout()
            relayout = true
        }
        if (relayout) return false

        // The clock keeps its size and position; it just draws itself into the part the sheet
        // doesn't cover. That's cheaper than a layout pass on every frame of a drag.
        clock.coveredBottom = (content.top + clock.bottom + clockGap - sheet.top).coerceIn(0, clock.height)
        if (behavior.state != shownState) show(behavior.state)
        return true
    }

    private fun show(state: Int) {
        shownState = state
        val collapsed = state == BottomSheetBehavior.STATE_COLLAPSED
        back.isEnabled = !collapsed
        val label = header.context.getString(if (collapsed) R.string.designs_show_all else R.string.designs_show_fewer)
        // TalkBack reads this as "double-tap to show all designs", since dragging is hard
        // without sight.
        ViewCompat.replaceAccessibilityAction(header, AccessibilityActionCompat.ACTION_CLICK, label, null)
        // A sheet collapsed while scrolled down would peek at a group other than the first.
        if (collapsed) picker.scrollTo(0, 0)
    }

    private companion object {
        const val NO_STATE = 0
        const val COLLAPSED_MAX_SHARE = 0.5f
        const val EXPANDED_MAX_SHARE = 0.7f
        const val CLOCK_GAP_DP = 16
        const val HINT_DELAY_MS = 1000L
        const val HOP_DP = 48
        const val RISE_MS = 450
        const val HOLD_MS = 50
        const val FALL_MS = 340
        const val LANDING_MS = RISE_MS + HOLD_MS + FALL_MS
        const val MORPH_SNAP = 2f

        // Each bounce's height as a share of the full lift. Under constant gravity, the time
        // to rise or fall grows with the square root of the height, which sets BOUNCE_MS (the
        // time up, which is also the time back down).
        val BOUNCE_HEIGHTS = floatArrayOf(0.16f, 0.04f)
        val BOUNCE_MS = BOUNCE_HEIGHTS.map { (FALL_MS * sqrt(it)).roundToInt() }
        val HOP_MS = LANDING_MS + 2 * BOUNCE_MS.sum()
    }
}
