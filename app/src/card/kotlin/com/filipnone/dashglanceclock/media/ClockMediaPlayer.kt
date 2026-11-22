package com.filipnone.dashglanceclock.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.text.format.DateFormat
import androidx.annotation.OptIn
import androidx.core.graphics.createBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.filipnone.dashglanceclock.R
import com.filipnone.dashglanceclock.clock.ClockDesign
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// A player that plays nothing. Its only media item is the current time, so Android Auto's
// media card shows a clock in the design picked on the phone. Play/pause and skip go to the
// music player that's active on the phone, through MusicRemote.
@OptIn(UnstableApi::class)
class ClockMediaPlayer(
    private val context: Context,
    private val music: MusicRemote,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) : SimpleBasePlayer(Looper.getMainLooper()) {

    private val preferences = CardDesigns.preferences(context)
    private val designs = CardDesigns(context)
    private val cardArtwork = CardArtwork()
    private var artworkKey: ArtworkKey? = null
    private var artwork = ByteArray(0)

    // Call when the minute, the chosen design or the phone's music changes.
    fun refresh() = invalidateState()

    override fun getState(): State {
        val time = now().truncatedTo(ChronoUnit.MINUTES)
        val playing = music.nowPlaying

        val metadata = MediaMetadata.Builder()
            .setTitle(title(time))
            .setArtist(subtitle(playing))
            .setArtworkData(
                artworkFor(ArtworkKey(time, designs.chosen(preferences), DateFormat.is24HourFormat(context))),
                MediaMetadata.PICTURE_TYPE_FRONT_COVER,
            )
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
        val item = MediaItem.Builder()
            .setMediaId(CLOCK_ITEM_ID)
            .setMediaMetadata(metadata)
            .build()

        val commands = Player.Commands.Builder().addAll(
            COMMAND_GET_CURRENT_MEDIA_ITEM,
            COMMAND_GET_METADATA,
            COMMAND_GET_TIMELINE,
        )
        if (playing != null) {
            commands.addAll(COMMAND_PLAY_PAUSE, COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_PREVIOUS)
        }

        return State.Builder()
            .setAvailableCommands(commands.build())
            .setPlaylist(listOf(MediaItemData.Builder(CLOCK_ITEM_ID).setMediaItem(item).build()))
            .setPlaybackState(STATE_READY)
            .setPlayWhenReady(playing?.isPlaying == true, PLAY_WHEN_READY_CHANGE_REASON_REMOTE)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) music.play() else music.pause()
        return Futures.immediateVoidFuture()
    }

    // The playlist only holds the clock, so Media3 can't skip within it. Every next or previous
    // request goes to the phone's player instead, whatever index Media3 worked out.
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        when (seekCommand) {
            COMMAND_SEEK_TO_NEXT -> music.skipToNext()
            COMMAND_SEEK_TO_PREVIOUS -> music.skipToPrevious()
            else -> Unit
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()

    private fun subtitle(playing: NowPlaying?): String = when {
        !music.isAllowed -> context.getString(R.string.media_needs_access)
        playing == null -> ""
        else -> listOfNotNull(playing.title, playing.artist).joinToString(" · ")
    }

    // Android Auto stretches the artwork over the whole card and puts the title and buttons
    // on its lower half. The card is always dark, so designs use their dark colors.
    private fun artworkFor(key: ArtworkKey): ByteArray {
        if (key == artworkKey) return artwork
        val bitmap = createBitmap(ARTWORK_SIZE, ARTWORK_SIZE)
        val palette = key.design.palette(context, isDarkMode = true)
        cardArtwork.draw(Canvas(bitmap), 0f, 0f, ARTWORK_SIZE.toFloat(), key.time, key.design, palette, key.use24Hour)

        artwork = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        bitmap.recycle()
        artworkKey = key
        return artwork
    }

    private data class ArtworkKey(val time: LocalDateTime, val design: ClockDesign, val use24Hour: Boolean)

    companion object {
        const val DATE_PATTERN = "EEEE d MMMM"
        private const val CLOCK_ITEM_ID = "clock"

        // Analog dials have thin lines, so the picture is 640 px. Android's media session
        // scales down any artwork wider than 320 dp before other apps get it.
        private const val ARTWORK_SIZE = 640

        // The card's title, such as "Sunday 22 November", in the phone's language.
        fun title(time: LocalDateTime): String =
            time.format(DateTimeFormatter.ofPattern(DATE_PATTERN)).replaceFirstChar { it.titlecase() }
    }
}
