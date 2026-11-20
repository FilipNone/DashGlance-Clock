package com.filipnone.dashglanceclock.clock

import android.content.Context
import android.graphics.Typeface

enum class DesignGroup { DASHGLANCE, PHONE_BRANDS, VINTAGE_CARS, MODERN_CARS, RETRO_SCIFI }

// How often a design needs drawing. NONE changes once a second. CONTINUOUS moves all the time.
// EACH_MINUTE moves for the first FLIP_MILLIS of every minute, while a flip clock's cards turn.
enum class Motion {
    NONE,
    CONTINUOUS,
    EACH_MINUTE,
    ;

    companion object {
        const val FLIP_MILLIS = 500L
    }
}

sealed interface DesignColors {
    data class Fixed(val day: ClockPalette, val night: ClockPalette = day) : DesignColors

    // Taken from the phone's wallpaper on Android 12 and newer.
    data object MaterialYou : DesignColors
}

sealed interface ClockDesign {
    val id: String
    val label: String
    val group: DesignGroup
    val colors: DesignColors
    val face: ClockFace

    val motion: Motion get() = Motion.NONE

    fun palette(context: Context, isDarkMode: Boolean): ClockPalette = when (val colors = colors) {
        is DesignColors.Fixed -> if (isDarkMode) colors.night else colors.day
        DesignColors.MaterialYou -> MaterialYouPalette.resolve(context, isDarkMode)
    }
}

// How digits are drawn. All three kinds are sized by digit height, so they can swap places
// without changing the layout code. A ghost draws the unlit segments or dots faintly, the way
// VFD and LED displays show them. For segments, gap is the space between two segments and
// spacing the space between two digits. Mesh covers the lit segments with the honeycomb grid
// of a VFD.
sealed interface Glyphs {
    data class Font(
        val family: String = "sans-serif-light",
        val style: Int = Typeface.NORMAL,
        val scaleX: Float = 1f,
        val letterSpacing: Float = 0f,
        val gap: Float = 0f,
    ) : Glyphs

    data class Segments(
        val thickness: Float = 0.12f,
        val slant: Float = 0f,
        val ghost: Boolean = false,
        val gap: Float = 0.02f,
        val spacing: Float = 0.14f,
        val mesh: Boolean = false,
    ) : Glyphs

    data class DotMatrix(
        val square: Boolean = false,
        val dotSize: Float = 0.8f,
        val ghost: Boolean = false,
    ) : Glyphs

    // A nixie tube per digit: the lit numeral glows in front of the other nine, which show as
    // faint wires, behind a honeycomb anode mesh.
    data class Nixie(
        val font: Font = Font("sans-serif-light"),
        val spacing: Float = 0.3f,
        val mesh: Boolean = true,
    ) : Glyphs

    // Angular digits drawn as thick strokes with sharp mitred corners, which turn into spikes
    // where a stroke doubles back. Digits only.
    data class Blade(
        val thickness: Float = 0.16f,
        val slant: Float = 0.2f,
        val spacing: Float = 0.12f,
    ) : Glyphs
}

// SHORT is "FRI 20 NOV", MEDIUM "Fri, Nov 20", LONG "Friday, November 20", and FULL adds the
// year. MEDIUM and LONG follow the word order of the phone's language. STAMP is a camcorder's
// "NOV. 20 2026", always in English.
enum class DateStyle { SHORT, MEDIUM, LONG, FULL, STAMP }

// Analog sizes and distances are fractions of the dial radius. The defaults draw the DashGlance
// face. Marks go at every minute, hour, quarter hour and 12, and each kind can be left out.
data class AnalogDesign(
    override val id: String,
    override val label: String,
    override val group: DesignGroup,
    override val colors: DesignColors,
    val dial: DialStyle = DialStyle(),
    val rings: List<Ring> = emptyList(),
    val band: Band? = null,
    val minuteMarks: Mark? = Mark.Line(start = 0.89f, end = 0.94f, width = 0.01f, ink = Ink.TICKS),
    val hourMarks: Mark? = Mark.Line(start = 0.84f, end = 0.94f, width = 0.025f, ink = Ink.HANDS),
    val quarterMarks: Mark? = hourMarks,
    val twelveMark: Mark? = quarterMarks,
    val numerals: Numerals? = Numerals(),
    val legend: Legend? = null,
    val date: DialText? = DialText(distance = 0.4f, size = 0.064f),
    val hourHand: Hand = Hand(HandShape.LINE, length = 0.5f, width = 0.05f, tail = 0.08f),
    val minuteHand: Hand = Hand(HandShape.LINE, length = 0.76f, width = 0.032f, tail = 0.08f),
    // The hub cap goes under the second hand, the other caps on top of it.
    val hubCap: Cap? = null,
    val secondHand: SecondHand? = SecondHand.Line(length = 0.86f, width = 0.012f, tail = 0.16f),
    val caps: List<Cap> = listOf(Cap(radius = 0.035f, ink = Ink.ACCENT)),
    val knob: Knob? = null,
) : ClockDesign {
    override val face: ClockFace get() = ClockFace.ANALOG
}

// SCALLOP is a circle with a wavy edge, like Material 3's cookie shape.
enum class DialShape { CIRCLE, SQUIRCLE, SCALLOP, RECTANGLE }

// The bezel is a ring around the dial, as a fraction of the radius. Everything else is measured
// from the dial inside it. Two different bezel colors make it shine like chrome, and two
// different dial colors give the dial a radial gradient. Aspect is width over height and only
// applies to RECTANGLE, whose radius is half its height. Its bezel is a mitred frame, or a
// plastic case with rounded corners when corner, a fraction of the half height, is above 0.
data class DialStyle(
    val shape: DialShape = DialShape.CIRCLE,
    val bezel: Float = 0f,
    val aspect: Float = 1f,
    val corner: Float = 0f,
)

// A thin circle, such as one side of a minute track. A distance above 1 lands on the bezel.
// With dots above 0 the circle is that many round dots instead of a line.
data class Ring(val distance: Float, val width: Float, val ink: Ink, val alpha: Float = 1f, val dots: Int = 0)

// A brushed metal bar across the center. Width and height are fractions of the dial's.
data class Band(val width: Float, val height: Float, val ink: Ink)

sealed interface Mark {
    val ink: Ink

    data class Line(
        val start: Float,
        val end: Float,
        val width: Float,
        override val ink: Ink,
        val round: Boolean = true,
    ) : Mark

    data class Dot(val distance: Float, val radius: Float, override val ink: Ink) : Mark

    data class Triangle(val start: Float, val end: Float, val width: Float, override val ink: Ink) : Mark

    data class Square(val distance: Float, val size: Float, override val ink: Ink) : Mark
}

enum class NumeralSet { ALL, QUARTERS }

data class Numerals(
    val set: NumeralSet = NumeralSet.ALL,
    val glyphs: Glyphs = Glyphs.Font(),
    val height: Float = 0.1f,
    val distance: Float = 0.69f,
    val ink: Ink = Ink.PRIMARY,
    val roman: Boolean = false,
)

data class DialText(
    val distance: Float,
    val size: Float,
    val ink: Ink = Ink.SECONDARY,
    val font: Glyphs.Font = Glyphs.Font(),
    val style: DateStyle = DateStyle.SHORT,
    val uppercase: Boolean = true,
)

data class Legend(val text: String, val look: DialText)

// LANCE and ARROW are thin at the hub and widest near the tip. ARROW has the shorter point.
// SPADE is a thin stem ending in a teardrop, round on the hub side and tapering into the point.
enum class HandShape { LINE, BATON, NEEDLE, PENCIL, LEAF, NECKED, SPADE, LANCE, ARROW }

// The tail reaches past the center on the opposite side. Lume is a strip of luminous paint
// down the outer half of the hand.
data class Hand(
    val shape: HandShape,
    val length: Float,
    val width: Float,
    val tail: Float = 0f,
    val ink: Ink = Ink.HANDS,
    val lume: Ink? = null,
)

sealed interface SecondHand {
    data class Line(
        val length: Float,
        val width: Float,
        val tail: Float = 0f,
        val ink: Ink = Ink.ACCENT,
        val counterweight: Float = 0f,
    ) : SecondHand

    // A dot or triangle that travels around the dial instead of a hand. Size is the dot's
    // radius, or half the triangle's height.
    data class Orbit(
        val distance: Float,
        val size: Float,
        val ink: Ink = Ink.ACCENT,
        val triangle: Boolean = false,
    ) : SecondHand
}

enum class Finish { FLAT, DOME, CONE }

data class Cap(val radius: Float, val ink: Ink, val finish: Finish = Finish.FLAT)

// The time-setting knob below the 6, drawn as a chrome dome on top of everything else.
data class Knob(val distance: Float, val radius: Float, val ink: Ink)

// Digital sizes and gaps are multiples of the digit height. The defaults draw the DashGlance
// face.
data class DigitalDesign(
    override val id: String,
    override val label: String,
    override val group: DesignGroup,
    override val colors: DesignColors,
    val glyphs: Glyphs = Glyphs.Font(),
    val align: TextAlign = TextAlign.CENTER,
    val seconds: Boolean = true,
    val amPm: Boolean = true,
    val blinkColon: Boolean = false,
    val hoursInk: Ink = Ink.PRIMARY,
    val minutesInk: Ink = hoursInk,
    val colonInk: Ink = hoursInk,
    val secondsInk: Ink = Ink.ACCENT,
    val date: DigitalDate? = DigitalDate(),
    val rule: Rule? = null,
    val panel: Panel? = null,
    val cells: Cells? = null,
    val glow: Float = 0f,
    val colon: Boolean = true,
    val chroma: Chroma? = null,
    val glitch: Glitch? = null,
    val grid: Boolean = false,
    val scanlines: Boolean = false,
    val amPmBefore: Boolean = false,
    val flip: Boolean = false,
) : ClockDesign {
    override val face: ClockFace get() = ClockFace.DIGITAL
    override val motion: Motion
        get() = when {
            glitch != null || grid -> Motion.CONTINUOUS
            flip -> Motion.EACH_MINUTE
            else -> Motion.NONE
        }
}

enum class TextAlign { CENTER, START, END }

enum class DatePosition { ABOVE, BELOW }

// Align overrides the design's alignment for the date. END lines the date up with the right
// edge of the time. Glyphs replaces the font, for designs whose date uses the digits' own
// glyphs. Trace above 0 spreads the letters that far apart (in date heights) and runs circuit
// traces through the gaps.
data class DigitalDate(
    val style: DateStyle = DateStyle.FULL,
    val position: DatePosition = DatePosition.BELOW,
    val size: Float = 0.16f,
    val gap: Float = 0.45f,
    val ink: Ink = Ink.SECONDARY,
    val font: Glyphs.Font = Glyphs.Font(),
    val uppercase: Boolean = false,
    val align: TextAlign? = null,
    val glyphs: Glyphs? = null,
    val trace: Float = 0f,
)

// A line under the time. Its width is a fraction of the time's width.
data class Rule(val width: Float = 1f, val thickness: Float = 0.03f, val ink: Ink = Ink.ACCENT)

// A rounded box behind the whole face, like the glass in front of an old car display. Padding
// is above and below the content, side padding left and right. The border is drawn in
// borderInk inside the box's edge.
data class Panel(
    val padding: Float = 0.3f,
    val corner: Float = 0.12f,
    val sidePadding: Float = padding,
    val border: Float = 0f,
    val borderInk: Ink = Ink.SECONDARY,
    val chamfer: Boolean = false,
)

// A separate window behind each digit, as on an LED readout, or behind each number when whole
// is set. Split draws the line across the middle of a flip clock's cards. Cells use the
// palette's panel color.
data class Cells(
    val padding: Float = 0.06f,
    val corner: Float = 0.02f,
    val whole: Boolean = false,
    val split: Boolean = false,
)

// Copies of the digits nudged left and right in two other colors, like the color bleed of a
// VHS tape.
data class Chroma(val offset: Float, val left: Ink, val right: Ink)

// A broken-screen look. The digits get an edge in another color, offset down and to the left.
// A few times a second, slices of the time jump sideways and noise bars flash around it.
data class Glitch(val edge: Float = 0.035f, val edgeInk: Ink = Ink.ACCENT)
