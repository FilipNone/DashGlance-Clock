package com.filipnone.dashglanceclock.media

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Looper
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import com.filipnone.dashglanceclock.clock.ClockDesigns
import com.filipnone.dashglanceclock.clock.ClockFace
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en")
class ClockMediaPlayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val music = FakeMusic()
    private val player = ClockMediaPlayer(context, music) { LocalDateTime.of(2026, 11, 17, 22, 10, 37) }

    @After
    fun release() = player.release()

    @Test
    fun cardShowsTheDateAndTheSongPlayingOnThePhone() {
        music.nowPlaying = NowPlaying("Blue Blazes", "Yuzo Koshiro", isPlaying = true)
        player.refresh()
        idle()

        assertEquals("Tuesday 17 November", player.mediaMetadata.title.toString())
        assertEquals("Blue Blazes · Yuzo Koshiro", player.mediaMetadata.artist.toString())
        assertTrue(player.playWhenReady)
    }

    @Test
    fun artworkIsASquareClockPicture() {
        val artwork = player.mediaMetadata.artworkData
        assertNotNull(artwork)
        val bitmap = BitmapFactory.decodeByteArray(artwork, 0, artwork!!.size)

        assertEquals(bitmap.width, bitmap.height)
    }

    @Test
    fun artworkFollowsTheDesignPickedOnThePhone() {
        val preferences = CardDesigns.preferences(context)
        fun artwork(): ByteArray {
            player.refresh()
            idle()
            return player.mediaMetadata.artworkData!!
        }

        val digital = artwork()
        preferences.stacked = true
        val stacked = artwork()
        preferences.face = ClockFace.ANALOG
        val analog = artwork()
        preferences.select(ClockDesigns.all.single { it.id == "jaguar_1960s" })
        val jaguar = artwork()

        val pictures = listOf(digital, stacked, analog, jaguar)
        assertEquals(4, pictures.map { it.toList() }.toSet().size)
    }

    @Test
    fun artworkLeavesTheEdgesThatTheCardCropsEmpty() {
        CardDesigns.preferences(context).face = ClockFace.ANALOG
        player.refresh()
        idle()
        val artwork = player.mediaMetadata.artworkData!!
        val bitmap = BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
        val background = bitmap.getPixel(0, 0)

        // The card shows the middle 52% of the width, and its text covers the bottom 45%.
        val size = bitmap.width
        for (y in 0 until size) {
            for (x in 0 until size) {
                val shown = x >= size * 0.24f && x < size * 0.76f && y < size * 0.55f
                if (!shown) assertEquals("($x, $y)", background, bitmap.getPixel(x, y))
            }
        }
    }

    @Test
    fun withoutMusic_theButtonsAreHidden() {
        music.nowPlaying = null
        player.refresh()
        idle()

        assertFalse(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
        assertFalse(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
    }

    @Test
    fun withMusicControlsOff_theCardSaysSo() {
        music.isAllowed = false
        player.refresh()
        idle()

        assertEquals("Allow music controls in the DashGlance Card app", player.mediaMetadata.artist.toString())
    }

    @Test
    fun buttonsControlThePhonesPlayer() {
        music.nowPlaying = NowPlaying("Blue Blazes", "Yuzo Koshiro", isPlaying = false)
        player.refresh()
        idle()

        player.play()
        player.seekToNext()
        player.seekToPrevious()
        player.pause()
        idle()

        assertEquals(listOf("play", "next", "previous", "pause"), music.calls)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private class FakeMusic : MusicRemote {
        override var isAllowed = true
        override var nowPlaying: NowPlaying? = NowPlaying("Inorganic Soul", "Yuzo Koshiro", isPlaying = false)
        val calls = mutableListOf<String>()

        override fun play() {
            calls += "play"
        }

        override fun pause() {
            calls += "pause"
        }

        override fun skipToNext() {
            calls += "next"
        }

        override fun skipToPrevious() {
            calls += "previous"
        }
    }
}
