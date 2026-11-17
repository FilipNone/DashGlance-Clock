package com.filipnone.dashglanceclock.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat

data class NowPlaying(val title: String?, val artist: String?, val isPlaying: Boolean)

interface MusicRemote {
    val hasAccess: Boolean
    val nowPlaying: NowPlaying?
    fun play()
    fun pause()
    fun skipToNext()
    fun skipToPrevious()
}

class PhoneMusic(
    private val context: Context,
    private val onChange: () -> Unit,
) : MusicRemote {

    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaControlListener::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var listening = false

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { select(it.orEmpty()) }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = onChange()
        override fun onMetadataChanged(metadata: MediaMetadata?) = onChange()
        override fun onSessionDestroyed() = select(activeSessions())
    }

    override val hasAccess: Boolean
        get() = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    override val nowPlaying: NowPlaying?
        get() = controller?.let { player ->
            NowPlaying(
                title = player.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = player.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                isPlaying = player.playbackState?.state == PlaybackState.STATE_PLAYING,
            )
        }

    // Safe to call often. It starts listening once notification access is granted, and picks
    // the player again in case another one started without the session list changing.
    fun refresh() {
        if (!listening && hasAccess) {
            try {
                sessionManager.addOnActiveSessionsChangedListener(sessionsChanged, listenerComponent, handler)
                listening = true
            } catch (_: SecurityException) {
                return
            }
        }
        if (listening) select(activeSessions())
    }

    fun release() {
        if (listening) sessionManager.removeOnActiveSessionsChangedListener(sessionsChanged)
        listening = false
        controller?.unregisterCallback(controllerCallback)
        controller = null
    }

    override fun play() {
        controller?.transportControls?.play()
    }

    override fun pause() {
        controller?.transportControls?.pause()
    }

    override fun skipToNext() {
        controller?.transportControls?.skipToNext()
    }

    override fun skipToPrevious() {
        controller?.transportControls?.skipToPrevious()
    }

    private fun activeSessions(): List<MediaController> =
        try {
            sessionManager.getActiveSessions(listenerComponent)
        } catch (_: SecurityException) {
            emptyList()
        }

    private fun select(sessions: List<MediaController>) {
        val chosen = choosePlayer(
            sessions,
            isOwn = { it.packageName == context.packageName },
            isPlaying = { it.playbackState?.state == PlaybackState.STATE_PLAYING },
        )
        if (chosen?.sessionToken == controller?.sessionToken) return

        controller?.unregisterCallback(controllerCallback)
        controller = chosen
        chosen?.registerCallback(controllerCallback, handler)
        onChange()
    }
}

// Android lists sessions most recently active first. DashGlance's own session is in the list
// too, so it's skipped. A player that's playing right now beats one that's paused.
internal fun <T> choosePlayer(sessions: List<T>, isOwn: (T) -> Boolean, isPlaying: (T) -> Boolean): T? {
    val others = sessions.filterNot(isOwn)
    return others.firstOrNull(isPlaying) ?: others.firstOrNull()
}
