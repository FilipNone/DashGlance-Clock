package com.filipnone.dashglanceclock.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChoosePlayerTest {

    private data class Session(val name: String, val isOwn: Boolean = false, val isPlaying: Boolean = false)

    private fun choose(vararg sessions: Session) =
        choosePlayer(sessions.toList(), isOwn = { it.isOwn }, isPlaying = { it.isPlaying })?.name

    @Test
    fun prefersAPlayerThatIsPlaying() {
        assertEquals("spotify", choose(Session("podcasts"), Session("spotify", isPlaying = true)))
    }

    @Test
    fun fallsBackToTheMostRecentPlayer() {
        assertEquals("podcasts", choose(Session("podcasts"), Session("spotify")))
    }

    @Test
    fun neverChoosesDashGlancesOwnSession() {
        assertEquals("spotify", choose(Session("dashglance", isOwn = true, isPlaying = true), Session("spotify")))
        assertNull(choose(Session("dashglance", isOwn = true)))
    }

    @Test
    fun noSessions_meansNoPlayer() {
        assertNull(choose())
    }
}
