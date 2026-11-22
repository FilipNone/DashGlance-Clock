package com.filipnone.dashglanceclock.clock

enum class ClockFace {
    ANALOG,
    DIGITAL;

    fun toggled(): ClockFace = if (this == ANALOG) DIGITAL else ANALOG

    companion object {
        fun fromName(name: String?, default: ClockFace = ANALOG): ClockFace =
            entries.firstOrNull { it.name == name } ?: default
    }
}
