package com.filipnone.dashglanceclock.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper

data class NowPlaying(val title: String?, val artist: String?, val isPlaying: Boolean)

interface MusicRemote {
    // False until the user grants notification access, and while music controls are off in the app.
    val isAllowed: Boolean
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
    private val controls = MusicControls(context)
    private var controller: MediaController? = null
    private var listening = false

    // The car card follows the in-app switch straight away instead of at the next minute.
    init {
        controls.observe(::refresh)
    }

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { select(it.orEmpty()) }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = onChange()
        override fun onMetadataChanged(metadata: MediaMetadata?) = onChange()
        override fun onSessionDestroyed() = select(activeSessions())
    }

    override val isAllowed: Boolean
        get() = controls.isOn

    override val nowPlaying: NowPlaying?
        get() = controller?.let { player ->
            NowPlaying(
                title = player.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = player.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                isPlaying = player.playbackState?.state == PlaybackState.STATE_PLAYING,
            )
        }

    // Safe to call often. It starts listening once music controls are allowed and stops when
    // they aren't, and picks the player again in case another one started without the session
    // list changing.
    fun refresh() {
        if (!isAllowed) {
            if (listening || controller != null) {
                stopListening()
                onChange()
            }
            return
        }
        if (!listening) {
            try {
                sessionManager.addOnActiveSessionsChangedListener(sessionsChanged, listenerComponent, handler)
                listening = true
            } catch (_: SecurityException) {
                return
            }
            // The card stops asking for access even if nothing is playing yet.
            onChange()
        }
        select(activeSessions())
    }

    fun release() {
        controls.stopObserving()
        stopListening()
    }

    private fun stopListening() {
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
