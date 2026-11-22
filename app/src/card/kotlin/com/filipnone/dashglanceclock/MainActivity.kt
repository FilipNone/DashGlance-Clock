package com.filipnone.dashglanceclock

import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import android.widget.TextClock
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.filipnone.dashglanceclock.clock.ClockPalette
import com.filipnone.dashglanceclock.media.ClockMediaPlayer
import com.filipnone.dashglanceclock.media.MediaControlListener
import com.filipnone.dashglanceclock.media.MusicControls
import com.filipnone.dashglanceclock.ui.showCredit
import com.filipnone.dashglanceclock.ui.tryStart

// DashGlance Card's phone screen. The clock lives in the car, so the phone shows the time and
// date the card shows, says where to find it, and holds the music controls switch.
class MainActivity : ComponentActivity() {

    private lateinit var mediaAccess: CompoundButton
    private lateinit var musicControls: MusicControls

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root)
        val preview = findViewById<View>(R.id.preview)
        val time = findViewById<TextClock>(R.id.time)
        val date = findViewById<TextClock>(R.id.date)
        val hint = findViewById<TextView>(R.id.hint)
        val credit = findViewById<TextView>(R.id.credit)
        mediaAccess = findViewById(R.id.media_access)
        musicControls = MusicControls(this)

        val isDarkMode = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val palette = ClockPalette.forDarkMode(isDarkMode)
        root.setBackgroundColor(palette.background)
        preview.background = GradientDrawable().apply {
            setColor(palette.dial)
            cornerRadius = PREVIEW_CORNER_DP * resources.displayMetrics.density
        }
        time.setTextColor(palette.primaryText)
        date.setTextColor(palette.secondaryText)
        hint.setTextColor(palette.secondaryText)
        mediaAccess.setTextColor(palette.primaryText)
        credit.showCredit(palette.accent)

        // The card's own date pattern, so the phone and the car agree.
        date.format12Hour = ClockMediaPlayer.DATE_PATTERN
        date.format24Hour = ClockMediaPlayer.DATE_PATTERN

        // Turning the switch off only turns music controls off in the app; the notification
        // access stays granted. Turning it on opens Android's settings only if access was never
        // granted or has been revoked there, and onResume then shows what the user chose.
        // Setting the switch to the current state doesn't count as a flip.
        mediaAccess.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == musicControls.isOn) return@setOnCheckedChangeListener
            musicControls.isEnabled = isChecked
            if (isChecked && !musicControls.hasAccess) openNotificationAccessSettings()
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    // The user may have just come back from the settings screen, with access turned on or off.
    override fun onResume() {
        super.onResume()
        mediaAccess.isChecked = musicControls.isOn
    }

    private fun openNotificationAccessSettings() {
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(this, MediaControlListener::class.java).flattenToString(),
            )
        } else {
            null
        }
        val opened = (detail != null && tryStart(detail)) ||
            tryStart(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        // With no settings screen to open, nothing can change, so the switch flips back.
        if (!opened) mediaAccess.isChecked = musicControls.isOn
    }

    private companion object {
        const val PREVIEW_CORNER_DP = 24f
    }
}
