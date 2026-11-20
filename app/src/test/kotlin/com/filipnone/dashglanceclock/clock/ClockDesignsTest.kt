package com.filipnone.dashglanceclock.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockDesignsTest {

    @Test
    fun designIdsAreUnique() {
        val ids = ClockDesigns.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun eachFaceListsOnlyItsOwnDesigns_defaultFirst() {
        for (face in ClockFace.entries) {
            val designs = ClockDesigns.forFace(face)
            assertSame(ClockDesigns.default(face), designs.first())
            assertTrue(designs.all { it.face == face })
        }
    }

    @Test
    fun designsAreGroupedByOrigin() {
        assertEquals(
            listOf(DesignGroup.DASHGLANCE, DesignGroup.PHONE_BRANDS, DesignGroup.VINTAGE_CARS),
            ClockDesigns.analog.map { it.group }.distinct(),
        )
        assertEquals(DesignGroup.entries, ClockDesigns.digital.map { it.group }.distinct())
        assertEquals(
            listOf("nixie_tube", "cyberpunk_2077", "tron_legacy", "camcorder", "flip_clock"),
            ClockDesigns.digital.filter { it.group == DesignGroup.RETRO_SCIFI }.map { it.id },
        )
    }

    @Test
    fun phoneBrandsComeInBothFaces() {
        val analog = ClockDesigns.analog.filter { it.group == DesignGroup.PHONE_BRANDS }.map { it.label }
        val digital = ClockDesigns.digital.filter { it.group == DesignGroup.PHONE_BRANDS }.map { it.label }
        assertEquals(analog, digital)
        for (brand in listOf("Material You", "Apple", "Samsung", "Xiaomi", "Nothing")) {
            assertTrue(brand, analog.any { it.startsWith(brand) })
        }
    }

    @Test
    fun onlyTheGlitchGridAndFlipDesignsMove() {
        assertEquals(
            listOf("cyberpunk_2077", "tron_legacy"),
            ClockDesigns.all.filter { it.motion == Motion.CONTINUOUS }.map { it.id },
        )
        assertEquals(
            listOf("flip_clock"),
            ClockDesigns.all.filter { it.motion == Motion.EACH_MINUTE }.map { it.id },
        )
    }

    @Test
    fun find_returnsTheDesignWithThatId() {
        val design = ClockDesigns.find(ClockFace.DIGITAL, "nothing_digital")

        assertEquals("nothing_digital", design.id)
    }

    @Test
    fun find_fallsBackToTheDefault_forUnknownOrOtherFaceIds() {
        for (id in listOf(null, "", "no_such_design", "nothing_digital")) {
            assertSame(ClockDesigns.DashGlanceAnalog, ClockDesigns.find(ClockFace.ANALOG, id))
        }
    }
}
