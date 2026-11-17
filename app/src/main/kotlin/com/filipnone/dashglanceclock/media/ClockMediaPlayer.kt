package com.filipnone.dashglanceclock.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
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
import com.filipnone.dashglanceclock.clock.ClockPalette
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.min

// A player that plays nothing. Its only media item is the current time, so Android Auto's
// media card shows a clock. Play/pause and skip go to the music player that's active on
// the phone, through MusicRemote.
@OptIn(UnstableApi::class)
class ClockMediaPlayer(
    private val context: Context,
    private val music: MusicRemote,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) : SimpleBasePlayer(Looper.getMainLooper()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM")
    private val palette = ClockPalette.Dark
    // Android Auto dims card artwork to about 40% under its own overlay, so the digits are
    // pure white and a heavier weight than the car screen's clock.
    private val digitsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        fontFeatureSettings = "tnum"
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
    }
    private val digitBounds = Rect()
    private var artworkKey: String? = null
    private var artwork = ByteArray(0)

    // Call when the minute or the phone's music changes.
    fun refresh() = invalidateState()

    override fun getState(): State {
        val time = now().truncatedTo(ChronoUnit.MINUTES)
        val playing = music.nowPlaying

        val metadata = MediaMetadata.Builder()
            .setTitle(time.format(dateFormatter).replaceFirstChar { it.titlecase() })
            .setArtist(subtitle(playing))
            .setArtworkData(artworkFor(time.format(timeFormatter())), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
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

    private fun timeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm")

    private fun subtitle(playing: NowPlaying?): String = when {
        !music.hasAccess -> context.getString(R.string.media_needs_access)
        playing == null -> ""
        else -> listOfNotNull(playing.title, playing.artist).joinToString(" · ")
    }

    // Android Auto stretches the artwork over the whole card and puts the title and buttons
    // on its lower half, so the digits go in the upper middle of the square.
    private fun artworkFor(time: String): ByteArray {
        if (time == artworkKey) return artwork
        val bitmap = createBitmap(ARTWORK_SIZE, ARTWORK_SIZE)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.background)

        val maxWidth = ARTWORK_SIZE * DIGITS_MAX_WIDTH
        val maxHeight = ARTWORK_SIZE * DIGITS_MAX_HEIGHT
        digitsPaint.textSize = REFERENCE_TEXT_SIZE
        digitsPaint.getTextBounds(time, 0, time.length, digitBounds)
        digitsPaint.textSize = REFERENCE_TEXT_SIZE *
            min(maxWidth / digitsPaint.measureText(time), maxHeight / digitBounds.height())
        digitsPaint.getTextBounds(time, 0, time.length, digitBounds)
        canvas.drawText(time, ARTWORK_SIZE / 2f, ARTWORK_SIZE * DIGITS_CENTER_Y - digitBounds.exactCenterY(), digitsPaint)

        artwork = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        bitmap.recycle()
        artworkKey = time
        return artwork
    }

    private companion object {
        const val CLOCK_ITEM_ID = "clock"
        const val ARTWORK_SIZE = 320
        const val REFERENCE_TEXT_SIZE = 100f

        // Fractions of the artwork. The card crops the square to its own shape, and on a
        // 1280 x 720 screen only the middle 52% of the width stays visible. The card's text
        // covers the bottom 45%.
        const val DIGITS_CENTER_Y = 0.3f
        const val DIGITS_MAX_WIDTH = 0.46f
        const val DIGITS_MAX_HEIGHT = 0.26f
    }
}
