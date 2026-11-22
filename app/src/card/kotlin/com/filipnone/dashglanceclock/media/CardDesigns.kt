package com.filipnone.dashglanceclock.media

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.filipnone.dashglanceclock.clock.AnalogDesign
import com.filipnone.dashglanceclock.clock.ClockDesign
import com.filipnone.dashglanceclock.clock.ClockDesigns
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.ClockPreferences
import com.filipnone.dashglanceclock.clock.DesignColors
import com.filipnone.dashglanceclock.clock.DigitalDesign
import com.filipnone.dashglanceclock.clock.Glyphs
import com.filipnone.dashglanceclock.clock.Motion

// The designs DashGlance Card can show, in the form the card shows them.
class CardDesigns(private val context: Context) {

    private val fitting = HashMap<ClockFace, List<ClockDesign>>()

    fun forFace(face: ClockFace, stacked: Boolean): List<ClockDesign> =
        fitting.getOrPut(face) {
            ClockDesigns.forFace(face).map { redrawn[it.id] ?: it }.filter(::fitsTheCard)
        }.map { forCard(it, stacked) }

    // The saved design for the saved face, or DashGlance when the saved one can't go on the card.
    fun chosen(preferences: ClockPreferences): ClockDesign {
        val face = preferences.face
        val designs = forFace(face, preferences.stacked)
        val id = preferences.designFor(face).id
        return designs.firstOrNull { it.id == id } ?: designs.first()
    }

    // Designs that move all the time would freeze, since the card changes once a minute. The
    // rest need their hands or digits to stand out from the dial once Android Auto dims the card.
    fun fitsTheCard(design: ClockDesign): Boolean =
        design.motion != Motion.CONTINUOUS && contrastOnCard(design) >= MIN_CONTRAST

    // The contrast ratio of the hands or digits against what's behind them, as the card shows
    // them. Analog dials can fade toward the edge, so both dial colors count.
    fun contrastOnCard(design: ClockDesign): Double {
        val palette = design.palette(context, isDarkMode = true)
        val inks: List<Int>
        val behind: List<Int>
        when (design) {
            is AnalogDesign -> {
                inks = listOf(palette.color(design.hourHand.ink), palette.color(design.minuteHand.ink))
                behind = listOf(palette.dial, palette.dialEdge).map { ColorUtils.compositeColors(it, palette.background) }
            }
            is DigitalDesign -> {
                inks = listOf(palette.color(design.hoursInk), palette.color(design.minutesInk))
                behind = listOf(
                    if (design.panel != null || design.cells != null) {
                        ColorUtils.compositeColors(palette.panel, palette.background)
                    } else {
                        palette.background
                    },
                )
            }
        }
        return inks.minOf { ink ->
            behind.minOf { back -> ColorUtils.calculateContrast(dimmed(ColorUtils.compositeColors(ink, back)), dimmed(back)) }
        }
    }

    companion object {
        // Android Auto dims the card's picture under a dark layer: black shows as 28 out of 255
        // and white as 97. At best that leaves a contrast of 2.7:1, and below 2:1 thin hands
        // and dim digits get hard to read at a glance.
        const val MIN_CONTRAST = 2.0
        private const val CARD_BLACK = 28
        private const val CARD_WHITE = 97

        fun preferences(context: Context) = ClockPreferences(context, fileName = "card", defaultFace = ClockFace.DIGITAL)

        // Lagonda's red LEDs measure 1.45:1 on the card, because red looks dark however bright
        // it is. Its card version draws each segment the way a lit LED looks up close, with a
        // pale core inside a full red glow, on darker cells. The digits also sit closer
        // together and are thicker, so they fill more of the narrow card.
        val LagondaForCard: DigitalDesign = ClockDesigns.digital.single { it.id == "lagonda_1976" }.let { lagonda ->
            val colors = lagonda.colors as DesignColors.Fixed
            val segments = lagonda.glyphs as Glyphs.Segments
            lagonda.copy(
                colors = DesignColors.Fixed(
                    colors.night.copy(primaryText = LED_CORE, glow = LED_RED, panel = LED_CELL),
                ),
                glyphs = segments.copy(thickness = 0.13f, spacing = 0.22f),
                cells = lagonda.cells?.copy(padding = 0.06f),
                glow = 0.07f,
            )
        }

        // Card versions of catalog designs. They keep the catalog id, so a saved choice
        // still finds them.
        private val redrawn: Map<String, ClockDesign> = listOf(LagondaForCard).associateBy { it.id }

        private const val LED_CORE = 0xFFFFB0A0.toInt()
        private const val LED_RED = 0xFFFF3324.toInt()
        private const val LED_CELL = 0xFF1A0403.toInt()

        // The card changes once a minute, so nothing on it counts seconds or blinks, and the
        // flip clock's cards don't turn. The date is the card's title, so the clock leaves it
        // out, and AM/PM would take width from the digits in the narrow card.
        fun forCard(design: ClockDesign, stacked: Boolean): ClockDesign = when (design) {
            is AnalogDesign -> design.copy(secondHand = null, date = null)
            is DigitalDesign -> design.copy(
                seconds = false,
                amPm = false,
                date = null,
                blinkColon = false,
                flip = false,
                stacked = stacked,
            )
        }

        @ColorInt
        private fun dimmed(@ColorInt color: Int): Int {
            fun channel(value: Int) = CARD_BLACK + (CARD_WHITE - CARD_BLACK) * value / 255
            return Color.rgb(channel(Color.red(color)), channel(Color.green(color)), channel(Color.blue(color)))
        }
    }
}
