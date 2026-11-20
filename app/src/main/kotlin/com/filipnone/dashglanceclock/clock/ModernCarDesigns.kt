package com.filipnone.dashglanceclock.clock

// Digital clocks in the style of current car screens. The names say what inspired a design.
// None of these companies is involved with DashGlance.
internal object ModernCarDesigns {

    private val tesla = DesignColors.Fixed(
        day = ClockPalette(
            background = rgb(0xF4F4F4),
            primaryText = rgb(0x171A20),
            secondaryText = rgb(0x5C5E62),
            accent = rgb(0x3E6AE1),
        ),
        night = ClockPalette(
            background = rgb(0x171A20),
            primaryText = rgb(0xEEEEEE),
            secondaryText = rgb(0x8E8E8E),
            accent = rgb(0x3E6AE1),
        ),
    )

    private val mercedes = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x03060C),
            primaryText = rgb(0xFFFFFF),
            secondaryText = rgb(0x7F98B2),
            accent = rgb(0x22C3F5),
            glow = rgb(0x22C3F5, alpha = 0x80),
        ),
    )

    private val bmw = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x0A0D12),
            primaryText = rgb(0xF2F4F7),
            secondaryText = rgb(0x8A94A3),
            accent = rgb(0x1C69D4),
        ),
    )

    private val audi = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0xFFFFFF),
            secondaryText = rgb(0x9B9B9B),
            accent = rgb(0xF50537),
        ),
    )

    private val porsche = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x0E0E12),
            primaryText = rgb(0xF2F2F2),
            secondaryText = rgb(0x9A9AA0),
            accent = rgb(0xD5001C),
            panel = rgb(0x1B1B21),
        ),
    )

    private val polestar = DesignColors.Fixed(
        day = ClockPalette(
            background = rgb(0xF0F0F0),
            primaryText = rgb(0x1E1E1E),
            secondaryText = rgb(0x6E6E6E),
            accent = rgb(0xB8904A),
        ),
        night = ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0xE6E6E6),
            secondaryText = rgb(0x8C8C8C),
            accent = rgb(0xD4AF6A),
        ),
    )

    private val hyundai = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x0D0F12),
            primaryText = rgb(0xE9EEF2),
            secondaryText = rgb(0x7F8A94),
            accent = rgb(0x00AAD2),
            unlit = rgb(0x191D22),
        ),
    )

    private val rivian = DesignColors.Fixed(
        day = ClockPalette(
            background = rgb(0xF5F2EB),
            primaryText = rgb(0x0F1214),
            secondaryText = rgb(0x5E6366),
            accent = rgb(0xC98A00),
        ),
        night = ClockPalette(
            background = rgb(0x0F1214),
            primaryText = rgb(0xF5F2EB),
            secondaryText = rgb(0x8F9496),
            accent = rgb(0xFFAD00),
        ),
    )

    val digital = listOf(
        DigitalDesign(
            id = "tesla",
            label = "Tesla",
            group = DesignGroup.MODERN_CARS,
            colors = tesla,
            glyphs = Glyphs.Font("sans-serif-medium"),
            seconds = false,
            date = DigitalDate(
                style = DateStyle.MEDIUM,
                size = 0.11f,
                gap = 0.4f,
                font = Glyphs.Font("sans-serif-medium", letterSpacing = 0.08f),
                uppercase = true,
            ),
        ),
        DigitalDesign(
            id = "mercedes_mbux",
            label = "Mercedes MBUX",
            group = DesignGroup.MODERN_CARS,
            colors = mercedes,
            glyphs = Glyphs.Font("sans-serif-thin"),
            colonInk = Ink.ACCENT,
            date = DigitalDate(
                style = DateStyle.LONG,
                size = 0.11f,
                gap = 0.42f,
                font = Glyphs.Font(letterSpacing = 0.12f),
                uppercase = true,
            ),
            glow = 0.05f,
        ),
        DigitalDesign(
            id = "bmw_idrive",
            label = "BMW iDrive",
            group = DesignGroup.MODERN_CARS,
            colors = bmw,
            glyphs = Glyphs.Font("sans-serif-condensed"),
            align = TextAlign.START,
            seconds = false,
            date = DigitalDate(
                style = DateStyle.MEDIUM,
                size = 0.12f,
                gap = 0.45f,
                font = Glyphs.Font("sans-serif-condensed", letterSpacing = 0.08f),
                uppercase = true,
            ),
            rule = Rule(width = 0.4f, thickness = 0.035f),
        ),
        DigitalDesign(
            id = "audi_virtual_cockpit",
            label = "Audi Virtual Cockpit",
            group = DesignGroup.MODERN_CARS,
            colors = audi,
            glyphs = Glyphs.Font("sans-serif", scaleX = 1.15f),
            date = DigitalDate(
                style = DateStyle.LONG,
                size = 0.11f,
                gap = 0.45f,
                font = Glyphs.Font("sans-serif", scaleX = 1.1f, letterSpacing = 0.1f),
                uppercase = true,
            ),
            rule = Rule(thickness = 0.02f),
        ),
        DigitalDesign(
            id = "porsche_taycan",
            label = "Porsche Taycan",
            group = DesignGroup.MODERN_CARS,
            colors = porsche,
            glyphs = Glyphs.Font("sans-serif-medium", letterSpacing = 0.02f),
            seconds = false,
            date = DigitalDate(style = DateStyle.MEDIUM, size = 0.13f, gap = 0.35f, font = Glyphs.Font("sans-serif")),
            panel = Panel(padding = 0.4f, corner = 0.5f),
        ),
        DigitalDesign(
            id = "polestar",
            label = "Polestar",
            group = DesignGroup.MODERN_CARS,
            colors = polestar,
            glyphs = Glyphs.Font("sans-serif-thin"),
            date = DigitalDate(
                style = DateStyle.MEDIUM,
                size = 0.1f,
                gap = 0.45f,
                font = Glyphs.Font(letterSpacing = 0.25f),
                uppercase = true,
            ),
        ),
        DigitalDesign(
            id = "hyundai_ioniq",
            label = "Hyundai Ioniq",
            group = DesignGroup.MODERN_CARS,
            colors = hyundai,
            glyphs = Glyphs.DotMatrix(square = true, dotSize = 0.82f, ghost = true),
            seconds = false,
            colonInk = Ink.ACCENT,
            date = DigitalDate(style = DateStyle.LONG, size = 0.13f, gap = 0.4f),
        ),
        DigitalDesign(
            id = "rivian",
            label = "Rivian",
            group = DesignGroup.MODERN_CARS,
            colors = rivian,
            seconds = false,
            date = DigitalDate(
                style = DateStyle.MEDIUM,
                position = DatePosition.ABOVE,
                size = 0.11f,
                gap = 0.3f,
                ink = Ink.ACCENT,
                font = Glyphs.Font("sans-serif-medium", letterSpacing = 0.2f),
                uppercase = true,
            ),
        ),
    )
}
