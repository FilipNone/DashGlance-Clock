package com.filipnone.dashglanceclock.media

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.filipnone.dashglanceclock.MainActivity
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

// SimpleBasePlayer and the notification hook are both marked unstable in Media3.
@OptIn(UnstableApi::class)
class ClockMediaService : MediaLibraryService() {

    private lateinit var music: PhoneMusic
    private lateinit var player: ClockMediaPlayer
    private lateinit var session: MediaLibrarySession
    private lateinit var nudge: SessionListNudge
    private lateinit var stopWatchingDesign: () -> Unit

    private val timeChanged = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            music.refresh()
            player.refresh()
        }
    }

    override fun onCreate() {
        super.onCreate()
        music = PhoneMusic(this) { player.refresh() }
        player = ClockMediaPlayer(this, music)
        session = MediaLibrarySession.Builder(this, player, LibraryCallback)
            .setSessionActivity(openPhoneApp())
            .build()
        nudge = SessionListNudge(this)
        // Without this, Android Auto often keeps the music app in the card for the whole drive.
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) nudge.nudge()
            }
        })
        music.refresh()
        // A design picked on the phone shows up in the car right away.
        stopWatchingDesign = CardDesigns.preferences(this).listen { player.refresh() }

        // ACTION_TIME_TICK arrives at the start of every minute.
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(this, timeChanged, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = session

    // The phone's music player already shows its own notification, and this service plays no
    // audio, so it never posts one or goes into the foreground.
    override fun onUpdateNotificationAsync(
        session: MediaSession,
        startInForegroundRequired: Boolean,
    ): ListenableFuture<Void?> = Futures.immediateVoidFuture()

    override fun onDestroy() {
        unregisterReceiver(timeChanged)
        stopWatchingDesign()
        nudge.release()
        music.release()
        session.release()
        player.release()
        super.onDestroy()
    }

    private fun openPhoneApp(): PendingIntent =
        PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    // Android Auto browses the library when the app is opened from its launcher. There is
    // nothing to browse, so the root is empty and the clock stays in the media card.
    private object LibraryCallback : MediaLibrarySession.Callback {
        private val root = MediaItem.Builder()
            .setMediaId("root")
            .setMediaMetadata(MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false).build())
            .build()

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(LibraryResult.ofItem(root, params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
    }
}
