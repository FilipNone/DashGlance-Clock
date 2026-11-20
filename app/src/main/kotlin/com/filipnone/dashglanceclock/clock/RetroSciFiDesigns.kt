package com.filipnone.dashglanceclock.clock

import android.graphics.Typeface

// Display technologies and screen clocks from films and games. As with the brand groups, these
// are our own drawings and the names only say what inspired them.
internal object RetroSciFiDesigns {

    private val nixie = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0xFFA347),
            secondaryText = rgb(0x8A5A2E),
            accent = rgb(0xFFA347),
            unlit = rgb(0x2E2A26),
            glow = rgb(0xFF5A00, alpha = 0xDD),
            panel = rgb(0x17120E),
        ),
    )

    private val cyberpunk = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0xF6D21C),
            secondaryText = rgb(0x2EE6F5),
            accent = rgb(0x2EE6F5),
            glow = rgb(0x2EE6F5, alpha = 0xCC),
        ),
    )

    private val tron = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0xC8FDFF),
            secondaryText = rgb(0x6FE8F5),
            accent = rgb(0x00D8FF),
            glow = rgb(0x00D8FF, alpha = 0xEE),
        ),
    )

    // The red and cyan are the color bleed of a VHS picture, drawn faintly either side of the text.
    private val camcorder = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0xF4F4F4),
            secondaryText = rgb(0xF4F4F4),
            accent = rgb(0xFF3B30, alpha = 0x90),
            hands = rgb(0x35D9FF, alpha = 0x90),
            glow = rgb(0xFFFFFF, alpha = 0x55),
        ),
    )

    private val flip = DesignColors.Fixed(
        ClockPalette(
            background = rgb(0x000000),
            primaryText = rgb(0xEDEDED),
            secondaryText = rgb(0x9A9A9A),
            accent = rgb(0xEDEDED),
            panel = rgb(0x1E1E1E),
        ),
    )

    val digital = listOf(
        DigitalDesign(
            id = "nixie_tube",
            label = "Nixie tube",
            group = DesignGroup.RETRO_SCIFI,
            colors = nixie,
            glyphs = Glyphs.Nixie(),
            seconds = false,
            amPm = false,
            date = null,
            cells = Cells(padding = 0.12f, corner = 0.28f),
            glow = 0.08f,
        ),
        DigitalDesign(
            id = "cyberpunk_2077",
            label = "Cyberpunk 2077",
            group = DesignGroup.RETRO_SCIFI,
            colors = cyberpunk,
            glyphs = Glyphs.Blade(thickness = 0.17f, slant = 0.22f, spacing = 0.1f),
            seconds = false,
            amPm = false,
            date = DigitalDate(
                style = DateStyle.SHORT,
                size = 0.15f,
                gap = 0.35f,
                ink = Ink.ACCENT,
                font = Glyphs.Font("monospace", Typeface.BOLD),
                uppercase = true,
                align = TextAlign.END,
                trace = 0.55f,
            ),
            glow = 0.05f,
            glitch = Glitch(edge = 0.035f, edgeInk = Ink.ACCENT),
            scanlines = true,
        ),
        DigitalDesign(
            id = "tron_legacy",
            label = "Tron Legacy",
            group = DesignGroup.RETRO_SCIFI,
            colors = tron,
            glyphs = Glyphs.Segments(thickness = 0.05f, gap = 0f, spacing = 0.24f),
            seconds = false,
            amPm = false,
            date = DigitalDate(
                style = DateStyle.SHORT,
                size = 0.1f,
                gap = 0.4f,
                font = Glyphs.Font("sans-serif-light", letterSpacing = 0.4f),
                uppercase = true,
            ),
            glow = 0.08f,
            grid = true,
        ),
        // The date and time a camcorder printed into the corner of its videos.
        DigitalDesign(
            id = "camcorder",
            label = "Camcorder",
            group = DesignGroup.RETRO_SCIFI,
            colors = camcorder,
            glyphs = Glyphs.DotMatrix(square = true, dotSize = 1f),
            seconds = false,
            amPmBefore = true,
            date = DigitalDate(
                style = DateStyle.STAMP,
                size = 0.75f,
                gap = 0.3f,
                ink = Ink.PRIMARY,
                uppercase = true,
                align = TextAlign.END,
                glyphs = Glyphs.DotMatrix(square = true, dotSize = 1f),
            ),
            glow = 0.02f,
            chroma = Chroma(offset = 0.04f, left = Ink.HANDS, right = Ink.ACCENT),
            scanlines = true,
        ),
        DigitalDesign(
            id = "flip_clock",
            label = "Flip clock",
            group = DesignGroup.RETRO_SCIFI,
            colors = flip,
            glyphs = Glyphs.Font("sans-serif-condensed", Typeface.BOLD, gap = 0.1f),
            seconds = false,
            amPm = false,
            date = null,
            cells = Cells(padding = 0.16f, corner = 0.08f, whole = true, split = true),
            colon = false,
            flip = true,
        ),
    )
}
