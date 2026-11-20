package com.filipnone.dashglanceclock.ui

import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.updatePadding
import com.filipnone.dashglanceclock.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.max
import kotlin.math.roundToInt

// The bottom sheet that holds the design picker. Collapsed, it shows the first group of designs.
// Pulled up, it shows all of them and the clock above shrinks to stay in view.
class DesignSheet(
    activity: ComponentActivity,
    private val sheet: View,
    private val header: View,
    private val picker: DesignPicker,
    private val content: View,
    private val clock: ClockView,
) {

    private val behavior = BottomSheetBehavior.from(sheet)
    private val coordinator = sheet.parent as View
    private val clockGap = (CLOCK_GAP_DP * sheet.resources.displayMetrics.density).roundToInt()
    private var shownState = NO_STATE

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
    }

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
    }
}
