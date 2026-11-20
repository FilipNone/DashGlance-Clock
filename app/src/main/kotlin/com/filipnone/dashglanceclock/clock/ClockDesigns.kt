package com.filipnone.dashglanceclock.clock

// Every design the phone can pick from. The picker shows them in list order, grouped by
// DesignGroup. Saved choices are stored by id, so renaming an id resets that choice.
object ClockDesigns {

    private val dashGlanceColors = DesignColors.Fixed(day = ClockPalette.Light, night = ClockPalette.Dark)

    val DashGlanceAnalog = AnalogDesign(
        id = "dashglance_analog",
        label = "DashGlance",
        group = DesignGroup.DASHGLANCE,
        colors = dashGlanceColors,
    )

    val DashGlanceDigital = DigitalDesign(
        id = "dashglance_digital",
        label = "DashGlance",
        group = DesignGroup.DASHGLANCE,
        colors = dashGlanceColors,
    )

    val analog: List<AnalogDesign> =
        listOf(DashGlanceAnalog) + PhoneDesigns.analog + VintageCarDesigns.analog

    val digital: List<DigitalDesign> =
        listOf(DashGlanceDigital) + PhoneDesigns.digital + VintageCarDesigns.digital + ModernCarDesigns.digital +
            RetroSciFiDesigns.digital

    val all: List<ClockDesign> = analog + digital

    fun forFace(face: ClockFace): List<ClockDesign> = when (face) {
        ClockFace.ANALOG -> analog
        ClockFace.DIGITAL -> digital
    }

    fun default(face: ClockFace): ClockDesign = when (face) {
        ClockFace.ANALOG -> DashGlanceAnalog
        ClockFace.DIGITAL -> DashGlanceDigital
    }

    fun find(face: ClockFace, id: String?): ClockDesign =
        forFace(face).firstOrNull { it.id == id } ?: default(face)
}
