package com.filipnone.dashglanceclock.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.doOnLayout
import androidx.core.widget.NestedScrollView
import com.filipnone.dashglanceclock.R
import com.filipnone.dashglanceclock.clock.ClockDesign
import com.filipnone.dashglanceclock.clock.ClockDesigns
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.ClockPalette
import com.filipnone.dashglanceclock.clock.DesignGroup
import kotlin.math.roundToInt

// Groups go down the page and each group's designs go sideways in their own row. The picker
// scrolls vertically as a nested scroller, so the bottom sheet around it knows when to drag
// and when to let the list scroll.
class DesignPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : NestedScrollView(context, attrs) {

    var onPick: ((ClockDesign) -> Unit)? = null

    private val groups = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val tiles = LinkedHashMap<String, View>()
    private var shownFace: ClockFace? = null
    private var shownChrome: ClockPalette? = null

    init {
        addView(groups, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    // Where the first group ends, measured from the picker's top. The sheet peeks this far.
    val firstGroupBottom: Int
        get() = groups.top + (groups.getChildAt(0)?.bottom ?: 0)

    // The tiles are only rebuilt when the face or colors change. Picking a design just moves
    // the outline, so the rows stay where the user scrolled them. After a rebuild, the list
    // scrolls down to the selected design's group if needed, and its row scrolls sideways to it.
    fun show(face: ClockFace, selected: ClockDesign, chrome: ClockPalette) {
        val rebuild = face != shownFace || chrome != shownChrome
        if (rebuild) {
            build(face, chrome)
            shownFace = face
            shownChrome = chrome
        }
        tiles.forEach { (id, tile) -> tile.isSelected = id == selected.id }
        if (rebuild) {
            tiles[selected.id]?.let { tile -> doOnLayout { scrollToTile(tile) } }
        }
    }

    private fun scrollToTile(tile: View) {
        val row = tile.parent as? View ?: return
        val scroller = row.parent as? HorizontalScrollView ?: return
        val section = scroller.parent as? View ?: return
        val center = row.left + tile.left + tile.width / 2
        scroller.scrollTo((center - scroller.width / 2).coerceAtLeast(0), 0)
        if (groups.top + section.bottom > height - paddingBottom) {
            scrollTo(0, groups.top + section.top)
        }
    }

    private fun build(face: ClockFace, chrome: ClockPalette) {
        groups.removeAllViews()
        tiles.clear()
        for ((group, designs) in ClockDesigns.forFace(face).groupBy { it.group }) {
            val section = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            section.addView(
                TextView(context).apply {
                    setText(group.titleRes())
                    setTextColor(chrome.secondaryText)
                    textSize = GROUP_TEXT_SP
                    setPadding(dp(ROW_INSET_DP + TILE_PADDING_DP), dp(8), dp(ROW_INSET_DP), dp(2))
                },
            )
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(ROW_INSET_DP), 0, dp(ROW_INSET_DP), 0)
            }
            for (design in designs) {
                val tile = tile(design, chrome)
                tiles[design.id] = tile
                row.addView(tile)
            }
            section.addView(
                HorizontalScrollView(context).apply {
                    isHorizontalScrollBarEnabled = false
                    addView(row)
                },
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT),
            )
            groups.addView(section, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
    }

    private fun tile(design: ClockDesign, chrome: ClockPalette): View {
        val tile = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(TILE_PADDING_DP), dp(TILE_PADDING_DP), dp(TILE_PADDING_DP), dp(TILE_PADDING_DP))
            background = selectionBackground(chrome)
            foreground = touchFeedback()
            isClickable = true
            isFocusable = true
            contentDescription = design.label
            setOnClickListener { onPick?.invoke(design) }
        }
        val preview = ClockView(context).apply {
            this.design = design
            cornerRadius = dp(12).toFloat()
            setPadding(dp(6), dp(6), dp(6), dp(6))
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        tile.addView(preview, LinearLayout.LayoutParams(dp(PREVIEW_WIDTH_DP), dp(PREVIEW_HEIGHT_DP)))
        val label = TextView(context).apply {
            text = design.label
            setTextColor(chrome.primaryText)
            textSize = LABEL_TEXT_SP
            gravity = Gravity.CENTER
            maxLines = 2
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        tile.addView(
            label,
            LinearLayout.LayoutParams(dp(PREVIEW_WIDTH_DP), WRAP_CONTENT).apply { topMargin = dp(4) },
        )
        return tile
    }

    private fun selectionBackground(chrome: ClockPalette): Drawable = StateListDrawable().apply {
        val outline = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setStroke(dp(2), chrome.accent)
        }
        addState(intArrayOf(android.R.attr.state_selected), outline)
    }

    private fun touchFeedback(): Drawable? {
        val attributes = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
        return try {
            attributes.getDrawable(0)
        } finally {
            attributes.recycle()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    @StringRes
    private fun DesignGroup.titleRes(): Int = when (this) {
        DesignGroup.DASHGLANCE -> R.string.design_group_original
        DesignGroup.PHONE_BRANDS -> R.string.design_group_phones
        DesignGroup.VINTAGE_CARS -> R.string.design_group_vintage_cars
        DesignGroup.MODERN_CARS -> R.string.design_group_modern_cars
        DesignGroup.RETRO_SCIFI -> R.string.design_group_retro_scifi
    }

    private companion object {
        const val PREVIEW_WIDTH_DP = 104
        const val PREVIEW_HEIGHT_DP = 68
        const val TILE_PADDING_DP = 6
        const val ROW_INSET_DP = 12
        const val GROUP_TEXT_SP = 13f
        const val LABEL_TEXT_SP = 12f
    }
}
