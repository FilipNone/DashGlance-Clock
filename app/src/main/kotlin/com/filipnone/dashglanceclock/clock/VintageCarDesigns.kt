package com.filipnone.dashglanceclock.clock

import android.graphics.Typeface

// Our own drawings of dashboard clocks from old cars. Each name gives the make and era that
// inspired it. The digital ones imitate the LED, VFD and LCD readouts of the 1970s to 1990s.
internal object VintageCarDesigns {

    private val mercedes = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            dial = rgb(0x1C1C1C),
            ticks = rgb(0xF0F0F0),
            hands = rgb(0xF2861E),
            accent = rgb(0xF2861E),
            primaryText = rgb(0xF0F0F0),
            secondaryText = rgb(0xC4C4C4),
            bezel = rgb(0x3C3C3C),
            bezelShade = rgb(0x1A1A1A),
        ),
    )

    private val porsche = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            dial = rgb(0x131313),
            ticks = rgb(0xF2F2F2),
            hands = rgb(0xF4572A),
            accent = rgb(0xF2F2F2),
            primaryText = rgb(0xF2F2F2),
            secondaryText = rgb(0xA8A8A8),
            bezel = rgb(0x2E2E2E),
            bezelShade = rgb(0x0A0A0A),
        ),
    )

    private val jaguar = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            dial = rgb(0x121212),
            ticks = rgb(0xF2F2EE),
            hands = rgb(0xF0EEE6),
            accent = rgb(0xCFCFCF),
            primaryText = rgb(0xF2F2EE),
            secondaryText = rgb(0x5C5C5C),
            bezel = rgb(0x1C1C1C),
            bezelShade = rgb(0x080808),
        ),
    )

    private val ferrari = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            dial = rgb(0x232527),
            ticks = rgb(0xD8D2BE),
            hands = rgb(0xEDE6D0),
            accent = rgb(0xE0281E),
            primaryText = rgb(0xD8D2BE),
            secondaryText = rgb(0xC4C4C4),
            bezel = rgb(0x2E2E2E),
            bezelShade = rgb(0x0C0C0C),
        ),
    )

    private val chevrolet = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            dial = rgb(0x1E2124),
            ticks = rgb(0xF0F0F0),
            hands = rgb(0xD6262E),
            accent = rgb(0xF2F2F2),
            primaryText = rgb(0xF0F0F0),
            secondaryText = rgb(0xCFC9BC),
            bezel = rgb(0xDADADA),
            bezelShade = rgb(0x5A5A5A),
        ),
    )

    private val rollsRoyce = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            dial = rgb(0x121212),
            ticks = rgb(0xF2F0EA),
            hands = rgb(0xF2EEE2),
            accent = rgb(0xBDBDBD),
            primaryText = rgb(0xF2F0EA),
            secondaryText = rgb(0xBDBDBD),
            bezel = rgb(0xF0F0F0),
            bezelShade = rgb(0x777777),
        ),
    )

    private val alarmClock = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            dial = rgb(0x161616),
            ticks = rgb(0xE6E6E2),
            hands = rgb(0xF4F4F0),
            accent = rgb(0xE5231B),
            primaryText = rgb(0xCDEFC2),
            secondaryText = rgb(0xD8D8D4),
            bezel = rgb(0xEEE5CC),
            bezelShade = rgb(0xA79E8A),
        ),
    )

    private val lagonda = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0xFF3324),
            secondaryText = rgb(0xA3241A),
            accent = rgb(0xFF3324),
            unlit = rgb(0x3E0B09),
            glow = rgb(0xFF3324, alpha = 0x66),
            panel = rgb(0x2C0706),
        ),
    )

    private val cadillac = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0xFF8C1A),
            secondaryText = rgb(0x9A9A9A),
            accent = rgb(0xFF8C1A),
            glow = rgb(0xFF8C1A, alpha = 0x88),
            panel = rgb(0x050505),
        ),
    )

    private val toyota = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0x63F0C2),
            secondaryText = rgb(0x3C9C7E),
            accent = rgb(0x63F0C2),
            glow = rgb(0x63F0C2, alpha = 0x88),
        ),
    )

    private val volkswagen = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0x1A1C18),
            secondaryText = rgb(0x4A4E44),
            accent = rgb(0x1A1C18),
            panel = rgb(0xB4BBA0),
        ),
    )

    val analog = listOf(
        AnalogDesign(
            id = "mercedes_1970s",
            label = "Mercedes 1970s",
            group = DesignGroup.VINTAGE_CARS,
            colors = mercedes,
            dial = DialStyle(bezel = 0.035f),
            minuteMarks = Mark.Line(start = 0.89f, end = 0.96f, width = 0.008f, ink = Ink.TICKS, round = false),
            hourMarks = Mark.Line(start = 0.84f, end = 0.96f, width = 0.022f, ink = Ink.TICKS, round = false),
            quarterMarks = Mark.Line(start = 0.78f, end = 0.96f, width = 0.026f, ink = Ink.TICKS, round = false),
            numerals = Numerals(glyphs = Glyphs.Font("sans-serif-condensed"), height = 0.12f, distance = 0.69f),
            date = null,
            hourHand = Hand(HandShape.PENCIL, length = 0.52f, width = 0.055f),
            minuteHand = Hand(HandShape.PENCIL, length = 0.86f, width = 0.045f),
            secondHand = null,
            caps = listOf(Cap(radius = 0.15f, ink = Ink.SECONDARY, finish = Finish.CONE)),
        ),
        AnalogDesign(
            id = "porsche_1970s",
            label = "Porsche 1970s",
            group = DesignGroup.VINTAGE_CARS,
            colors = porsche,
            dial = DialStyle(bezel = 0.07f),
            minuteMarks = Mark.Line(start = 0.86f, end = 0.95f, width = 0.018f, ink = Ink.TICKS, round = false),
            hourMarks = Mark.Line(start = 0.83f, end = 0.95f, width = 0.036f, ink = Ink.TICKS, round = false),
            quarterMarks = Mark.Line(start = 0.81f, end = 0.95f, width = 0.04f, ink = Ink.TICKS, round = false),
            numerals = Numerals(
                set = NumeralSet.QUARTERS,
                glyphs = Glyphs.Font("sans-serif"),
                height = 0.14f,
                distance = 0.66f,
            ),
            legend = Legend(
                "Quarz-Zeit",
                DialText(distance = 0.5f, size = 0.06f, ink = Ink.PRIMARY, font = Glyphs.Font("sans-serif"), uppercase = false),
            ),
            date = null,
            hourHand = Hand(HandShape.PENCIL, length = 0.5f, width = 0.06f),
            minuteHand = Hand(HandShape.PENCIL, length = 0.82f, width = 0.05f),
            hubCap = Cap(radius = 0.085f, ink = Ink.HANDS),
            secondHand = SecondHand.Line(length = 0.88f, width = 0.012f, tail = 0.15f),
            caps = listOf(
                Cap(radius = 0.06f, ink = Ink.SECONDARY),
                Cap(radius = 0.048f, ink = Ink.BACKGROUND, finish = Finish.DOME),
            ),
        ),
        AnalogDesign(
            id = "jaguar_1960s",
            label = "Jaguar 1960s",
            group = DesignGroup.VINTAGE_CARS,
            colors = jaguar,
            dial = DialStyle(bezel = 0.05f),
            rings = listOf(Ring(distance = 0.62f, width = 0.014f, ink = Ink.SECONDARY)),
            minuteMarks = null,
            hourMarks = Mark.Line(start = 0.8f, end = 0.95f, width = 0.028f, ink = Ink.TICKS, round = false),
            quarterMarks = null,
            numerals = Numerals(
                set = NumeralSet.QUARTERS,
                glyphs = Glyphs.Font("sans-serif"),
                height = 0.15f,
                distance = 0.84f,
            ),
            date = null,
            hourHand = Hand(HandShape.ARROW, length = 0.42f, width = 0.07f),
            minuteHand = Hand(HandShape.ARROW, length = 0.6f, width = 0.065f),
            secondHand = null,
            caps = listOf(Cap(radius = 0.045f, ink = Ink.ACCENT), Cap(radius = 0.025f, ink = Ink.DIAL)),
        ),
        AnalogDesign(
            id = "ferrari_1960s",
            label = "Ferrari 1960s",
            group = DesignGroup.VINTAGE_CARS,
            colors = ferrari,
            // The dial sits at the bottom of a deep black bezel, ringed with grooves and a
            // chrome rim. 1.5625 is the bezel's outer edge in dial radii.
            dial = DialStyle(bezel = 0.36f),
            rings = listOf(
                Ring(distance = 1.12f, width = 0.012f, ink = Ink.SECONDARY, alpha = 0.18f),
                Ring(distance = 1.24f, width = 0.012f, ink = Ink.SECONDARY, alpha = 0.18f),
                Ring(distance = 1.36f, width = 0.012f, ink = Ink.SECONDARY, alpha = 0.18f),
                Ring(distance = 1.54f, width = 0.035f, ink = Ink.SECONDARY),
            ),
            minuteMarks = null,
            hourMarks = Mark.Line(start = 0.68f, end = 0.9f, width = 0.018f, ink = Ink.TICKS, round = false),
            quarterMarks = Mark.Line(start = 0.9f, end = 0.98f, width = 0.02f, ink = Ink.TICKS, round = false),
            numerals = Numerals(
                set = NumeralSet.QUARTERS,
                glyphs = Glyphs.Font("sans-serif"),
                height = 0.16f,
                distance = 0.78f,
            ),
            date = null,
            hourHand = Hand(HandShape.PENCIL, length = 0.46f, width = 0.1f),
            minuteHand = Hand(HandShape.PENCIL, length = 0.8f, width = 0.045f),
            hubCap = Cap(radius = 0.08f, ink = Ink.HANDS),
            secondHand = SecondHand.Line(length = 0.86f, width = 0.016f, tail = 0.22f, counterweight = 0.03f),
            caps = listOf(Cap(radius = 0.045f, ink = Ink.ACCENT)),
        ),
        AnalogDesign(
            id = "chevrolet_1950s",
            label = "Chevrolet 1950s",
            group = DesignGroup.VINTAGE_CARS,
            colors = chevrolet,
            dial = DialStyle(shape = DialShape.RECTANGLE, bezel = 0.18f, aspect = 1.55f),
            band = Band(width = 0.56f, height = 0.26f, ink = Ink.SECONDARY),
            minuteMarks = null,
            hourMarks = Mark.Line(start = 0.62f, end = 0.94f, width = 0.014f, ink = Ink.TICKS, round = false),
            quarterMarks = null,
            numerals = Numerals(
                set = NumeralSet.QUARTERS,
                glyphs = Glyphs.Font("sans-serif-condensed-light"),
                height = 0.3f,
                distance = 0.76f,
            ),
            date = null,
            hourHand = Hand(HandShape.NEEDLE, length = 0.5f, width = 0.065f),
            minuteHand = Hand(HandShape.NEEDLE, length = 0.75f, width = 0.055f),
            secondHand = SecondHand.Line(length = 0.95f, width = 0.014f),
            caps = listOf(Cap(radius = 0.22f, ink = Ink.BEZEL, finish = Finish.CONE)),
            // Centered on the bottom of the frame, below the 6.
            knob = Knob(distance = 1.11f, radius = 0.1f, ink = Ink.BEZEL),
        ),
        AnalogDesign(
            id = "rolls_royce_1930s",
            label = "Rolls-Royce 1930s",
            group = DesignGroup.VINTAGE_CARS,
            colors = rollsRoyce,
            dial = DialStyle(bezel = 0.035f),
            // A railway track: two circles joined by a tick at every minute.
            rings = listOf(
                Ring(distance = 0.88f, width = 0.008f, ink = Ink.TICKS),
                Ring(distance = 0.8f, width = 0.008f, ink = Ink.TICKS),
            ),
            minuteMarks = Mark.Line(start = 0.8f, end = 0.88f, width = 0.007f, ink = Ink.TICKS, round = false),
            hourMarks = Mark.Line(start = 0.8f, end = 0.88f, width = 0.03f, ink = Ink.TICKS, round = false),
            quarterMarks = Mark.Line(start = 0.74f, end = 0.88f, width = 0.03f, ink = Ink.TICKS, round = false),
            numerals = Numerals(
                glyphs = Glyphs.Font("sans-serif-condensed", Typeface.BOLD),
                height = 0.16f,
                distance = 0.655f,
            ),
            legend = Legend(
                "ELECTRIC",
                DialText(distance = 0.4f, size = 0.05f, ink = Ink.PRIMARY, font = Glyphs.Font("sans-serif-medium", letterSpacing = 0.08f)),
            ),
            date = null,
            hourHand = Hand(HandShape.SPADE, length = 0.6f, width = 0.095f),
            minuteHand = Hand(HandShape.LANCE, length = 0.8f, width = 0.05f),
            secondHand = null,
            caps = listOf(Cap(radius = 0.04f, ink = Ink.SECONDARY, finish = Finish.DOME)),
        ),
        AnalogDesign(
            id = "initial_d_sb709",
            label = "Initial D SB709",
            group = DesignGroup.VINTAGE_CARS,
            colors = alarmClock,
            // A cream plastic alarm clock with a wide black dial and glow-in-the-dark numerals.
            dial = DialStyle(shape = DialShape.RECTANGLE, bezel = 0.2f, aspect = 1.48f, corner = 0.4f),
            rings = listOf(Ring(distance = 0.55f, width = 0.011f, ink = Ink.SECONDARY, alpha = 0.7f, dots = 90)),
            minuteMarks = Mark.Line(start = 0.89f, end = 0.95f, width = 0.008f, ink = Ink.TICKS, round = false),
            hourMarks = Mark.Square(distance = 0.92f, size = 0.04f, ink = Ink.TICKS),
            numerals = Numerals(glyphs = Glyphs.Font("sans-serif", scaleX = 0.8f), height = 0.28f, distance = 0.74f),
            legend = Legend(
                "QUARTZ ALARM",
                DialText(distance = 0.36f, size = 0.045f, font = Glyphs.Font("sans-serif", letterSpacing = 0.05f)),
            ),
            date = null,
            hourHand = Hand(HandShape.NECKED, length = 0.6f, width = 0.08f, lume = Ink.PRIMARY),
            minuteHand = Hand(HandShape.NECKED, length = 0.86f, width = 0.07f, lume = Ink.PRIMARY),
            secondHand = SecondHand.Line(length = 0.96f, width = 0.016f, tail = 0.12f),
            caps = listOf(Cap(radius = 0.11f, ink = Ink.ACCENT)),
        ),
    )

    val digital = listOf(
        DigitalDesign(
            id = "lagonda_1976",
            label = "Lagonda 1976",
            group = DesignGroup.VINTAGE_CARS,
            colors = lagonda,
            glyphs = Glyphs.Segments(thickness = 0.085f, slant = 0.17f, ghost = true, gap = 0.03f, spacing = 0.4f),
            seconds = false,
            amPm = false,
            blinkColon = true,
            date = null,
            cells = Cells(padding = 0.08f, corner = 0.02f),
            glow = 0.02f,
        ),
        DigitalDesign(
            id = "cadillac_1980s",
            label = "Cadillac 1980s",
            group = DesignGroup.VINTAGE_CARS,
            colors = cadillac,
            glyphs = Glyphs.Segments(thickness = 0.11f, slant = 0.15f, gap = 0.03f, spacing = 0.1f),
            seconds = false,
            amPm = false,
            date = null,
            panel = Panel(padding = 0.28f, corner = 0.04f, sidePadding = 0.6f, border = 0.035f),
            glow = 0.03f,
        ),
        DigitalDesign(
            id = "toyota_1980s",
            label = "Toyota 1980s",
            group = DesignGroup.VINTAGE_CARS,
            colors = toyota,
            glyphs = Glyphs.Segments(thickness = 0.12f, slant = 0.17f, gap = 0.035f, mesh = true),
            seconds = false,
            amPm = false,
            date = null,
            glow = 0.03f,
        ),
        DigitalDesign(
            id = "volkswagen_1990s",
            label = "Volkswagen 1990s",
            group = DesignGroup.VINTAGE_CARS,
            colors = volkswagen,
            glyphs = Glyphs.Segments(thickness = 0.09f, slant = 0.06f, gap = 0.015f),
            seconds = false,
            amPm = false,
            date = null,
            panel = Panel(padding = 0.22f, corner = 0.12f, sidePadding = 0.3f),
        ),
    )
}
