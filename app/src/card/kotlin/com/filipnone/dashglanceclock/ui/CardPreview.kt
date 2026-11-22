package com.filipnone.dashglanceclock.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withClip
import com.filipnone.dashglanceclock.clock.ClockDesign
import com.filipnone.dashglanceclock.clock.ClockDesigns
import com.filipnone.dashglanceclock.clock.ClockPalette
import com.filipnone.dashglanceclock.clock.SecondTicker
import com.filipnone.dashglanceclock.media.CardArtwork
import com.filipnone.dashglanceclock.media.ClockMediaPlayer
import java.time.LocalDateTime
import kotlin.math.min

// The media card as Android Auto shows it on a 1280 x 720 screen: tall and narrow, with the
// middle of the clock picture and the date near the bottom. It leaves out the card's dimming,
// so the design is easy to see, and like the card it's always dark.
class CardPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs), PartlyCovered {

    private val artwork = CardArtwork()
    private val ticker = SecondTicker { invalidate() }
    private val card = RectF()
    private val clip = Path()
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
        color = OUTLINE_COLOR
    }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        color = TITLE_COLOR
    }
    private var palette: ClockPalette = ClockPalette.Dark

    var design: ClockDesign = ClockDesigns.DashGlanceDigital
        set(value) {
            field = value
            palette = value.palette(context, isDarkMode = true)
            invalidate()
        }

    // The open design sheet covers the bottom, and the card shrinks into the part above it.
    override var coveredBottom: Int = 0
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) ticker.start() else ticker.stop()
    }

    override fun onDetachedFromWindow() {
        ticker.stop()
        super.onDetachedFromWindow()
    }

    // The artwork is as tall as the card and centered on it, so the card crops its sides.
    override fun onDraw(canvas: Canvas) {
        val roomWidth = (width - paddingLeft - paddingRight).toFloat()
        val roomHeight = (height - coveredBottom - paddingTop - paddingBottom).toFloat()
        val cardHeight = min(roomHeight, roomWidth / CARD_ASPECT)
        if (cardHeight <= 0f) return
        val cardWidth = cardHeight * CARD_ASPECT
        val centerX = paddingLeft + roomWidth / 2f
        val centerY = paddingTop + roomHeight / 2f
        card.set(centerX - cardWidth / 2f, centerY - cardHeight / 2f, centerX + cardWidth / 2f, centerY + cardHeight / 2f)
        val corner = cardHeight * CORNER
        clip.reset()
        clip.addRoundRect(card, corner, corner, Path.Direction.CW)

        val now = LocalDateTime.now()
        canvas.withClip(clip) {
            artwork.draw(this, centerX - cardHeight / 2f, card.top, cardHeight, now, design, palette, DateFormat.is24HourFormat(context))
            title.textSize = cardHeight * TITLE_SIZE
            drawText(ClockMediaPlayer.title(now), card.left + cardWidth * TITLE_LEFT, card.top + cardHeight * TITLE_BASELINE, title)
        }
        canvas.drawRoundRect(card, corner, corner, outline)
    }

    private companion object {
        // Measured on the Desktop Head Unit at 1280 x 720, where the card is 410 x 700 pixels.
        const val CARD_ASPECT = 410f / 700f
        const val CORNER = 0.034f
        const val TITLE_SIZE = 0.037f
        const val TITLE_LEFT = 0.04f
        const val TITLE_BASELINE = 0.8f
        const val TITLE_COLOR = 0xFFF1F1F1.toInt()

        // A faint edge, so the black card shows on a black screen.
        const val OUTLINE_COLOR = 0x33FFFFFF
    }
}
