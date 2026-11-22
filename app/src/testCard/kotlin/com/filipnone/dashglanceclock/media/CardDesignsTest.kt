package com.filipnone.dashglanceclock.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.filipnone.dashglanceclock.clock.AnalogDesign
import com.filipnone.dashglanceclock.clock.ClockDesigns
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.DigitalDesign
import com.filipnone.dashglanceclock.clock.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CardDesignsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val designs = CardDesigns(context)
    private val preferences = CardDesigns.preferences(context)

    @Test
    fun leavesOutTheAnimatedDesigns_andTheOnesTheDimmedCardWashesOut() {
        val left = ClockDesigns.all.map { it.id } -
            ClockFace.entries.flatMap { face -> designs.forFace(face, stacked = false).map { it.id } }.toSet()

        assertEquals(
            setOf(
                "cyberpunk_2077", "tron_legacy",
                "material_you_analog", "material_you_digital", "mercedes_1970s", "porsche_1970s", "chevrolet_1950s",
                "cadillac_1980s", "volkswagen_1990s", "nixie_tube",
            ),
            left.toSet(),
        )
    }

    @Test
    fun lagonda_isOnTheCardInItsCardVersion_whichTheDimmedCardKeepsReadable() {
        val catalog = ClockDesigns.digital.single { it.id == "lagonda_1976" }
        val onCard = designs.forFace(ClockFace.DIGITAL, stacked = false).single { it.id == "lagonda_1976" }

        assertFalse(designs.fitsTheCard(catalog))
        assertTrue(designs.fitsTheCard(CardDesigns.LagondaForCard))
        assertEquals("Lagonda 1976", onCard.label)
        assertEquals(CardDesigns.forCard(CardDesigns.LagondaForCard, stacked = false), onCard)
    }

    @Test
    fun aSavedLagonda_getsTheCardVersion() {
        preferences.face = ClockFace.DIGITAL
        preferences.select(ClockDesigns.digital.single { it.id == "lagonda_1976" })

        assertEquals(CardDesigns.LagondaForCard.colors, designs.chosen(preferences).colors)
    }

    @Test
    fun everyCardDesign_staysInTheVisiblePartOfTheCard_stackedOrNot() {
        val artwork = CardArtwork(Locale.US)
        val size = 320
        val visible = RectF(
            size * CardArtwork.CLOCK_LEFT,
            size * CardArtwork.CLOCK_TOP,
            size * CardArtwork.CLOCK_RIGHT,
            size * CardArtwork.CLOCK_BOTTOM,
        )
        // Dials reach the edge of their bounds, so their outlines may blur a pixel past it.
        visible.inset(-1f, -1f)
        val time = LocalDateTime.of(2026, 11, 18, 20, 48)
        for (face in ClockFace.entries) {
            for (stacked in listOf(false, true)) {
                for (design in designs.forFace(face, stacked)) {
                    val palette = design.palette(context, isDarkMode = true)
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    artwork.draw(Canvas(bitmap), 0f, 0f, size.toFloat(), time, design, palette, use24Hour = true)
                    var outside = 0
                    for (y in 0 until size) {
                        for (x in 0 until size) {
                            if (!visible.contains(x + 0.5f, y + 0.5f) && differs(bitmap.getPixel(x, y), palette.background)) {
                                outside++
                            }
                        }
                    }
                    assertEquals("${design.id} stacked=$stacked", 0, outside)
                }
            }
        }
    }

    // A faint glow edge may spill a little; anything brighter counts.
    private fun differs(pixel: Int, background: Int): Boolean =
        abs(Color.red(pixel) - Color.red(background)) > 24 ||
            abs(Color.green(pixel) - Color.green(background)) > 24 ||
            abs(Color.blue(pixel) - Color.blue(background)) > 24

    @Test
    fun everyDesignOnTheCard_keepsItsHandsOrDigitsReadable() {
        for (face in ClockFace.entries) {
            for (design in designs.forFace(face, stacked = false)) {
                assertTrue(design.id, designs.contrastOnCard(design) >= CardDesigns.MIN_CONTRAST)
                assertTrue(design.id, design.motion == Motion.NONE)
            }
        }
    }

    @Test
    fun theDashGlanceDesignsComeFirst() {
        assertEquals("dashglance_analog", designs.forFace(ClockFace.ANALOG, stacked = false).first().id)
        assertEquals("dashglance_digital", designs.forFace(ClockFace.DIGITAL, stacked = false).first().id)
    }

    @Test
    fun cardDesigns_leaveOutSecondsDateAndAmPm_whichTheCardCantKeepUpWithOrHasNoRoomFor() {
        for (design in designs.forFace(ClockFace.ANALOG, stacked = false)) {
            design as AnalogDesign
            assertNull(design.id, design.secondHand)
            assertNull(design.id, design.date)
        }
        for (design in designs.forFace(ClockFace.DIGITAL, stacked = false)) {
            design as DigitalDesign
            assertFalse(design.id, design.seconds || design.amPm || design.blinkColon || design.flip)
            assertNull(design.id, design.date)
        }
    }

    @Test
    fun stacked_onlyChangesTheDigitalDesigns() {
        assertTrue(designs.forFace(ClockFace.DIGITAL, stacked = true).all { (it as DigitalDesign).stacked })
        assertEquals(
            designs.forFace(ClockFace.ANALOG, stacked = false),
            designs.forFace(ClockFace.ANALOG, stacked = true),
        )
    }

    @Test
    fun theCardStartsOnTheDigitalDashGlanceClock_inOneRow() {
        val chosen = designs.chosen(preferences) as DigitalDesign

        assertEquals("dashglance_digital", chosen.id)
        assertFalse(chosen.stacked)
    }

    @Test
    fun theCard_keepsItsOwnChoices_apartFromTheOldAppsClock() {
        // 0.2 saved the full-screen clock's designs in "clock", and DashGlance Card keeps that file.
        context.getSharedPreferences("clock", Context.MODE_PRIVATE).edit {
            putString("face", "ANALOG")
            putString("analog_design", "jaguar_1960s")
        }

        assertEquals("dashglance_digital", designs.chosen(preferences).id)
    }

    @Test
    fun chosen_followsTheSavedFaceDesignAndStacking() {
        preferences.face = ClockFace.DIGITAL
        preferences.select(ClockDesigns.all.single { it.id == "toyota_1980s" })
        preferences.stacked = true

        val chosen = designs.chosen(preferences) as DigitalDesign
        assertEquals("toyota_1980s", chosen.id)
        assertTrue(chosen.stacked)
        assertFalse(chosen.seconds)
    }

    @Test
    fun aSavedDesignTheCardCantShow_fallsBackToDashGlance() {
        preferences.face = ClockFace.ANALOG
        preferences.select(ClockDesigns.all.single { it.id == "chevrolet_1950s" })

        assertEquals("dashglance_analog", designs.chosen(preferences).id)
    }
}
