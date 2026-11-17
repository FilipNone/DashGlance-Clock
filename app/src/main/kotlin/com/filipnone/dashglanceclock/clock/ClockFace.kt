package com.filipnone.dashglanceclock.clock

enum class ClockFace {
    ANALOG,
    DIGITAL;

    fun toggled(): ClockFace = if (this == ANALOG) DIGITAL else ANALOG

    companion object {
        fun fromName(name: String?): ClockFace = entries.firstOrNull { it.name == name } ?: ANALOG
    }
}
