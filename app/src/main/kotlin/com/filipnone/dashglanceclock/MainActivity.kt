package com.filipnone.dashglanceclock

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.filipnone.dashglanceclock.clock.ClockPalette
import com.filipnone.dashglanceclock.clock.ClockPreferences
import com.filipnone.dashglanceclock.media.MediaControlListener
import com.filipnone.dashglanceclock.ui.ClockView

class MainActivity : ComponentActivity() {

    private lateinit var mediaAccess: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root)
        val clock = findViewById<ClockView>(R.id.clock)
        val hint = findViewById<TextView>(R.id.hint)
        val credit = findViewById<TextView>(R.id.credit)
        mediaAccess = findViewById(R.id.media_access)

        val isDarkMode = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val palette = ClockPalette.forDarkMode(isDarkMode)
        root.setBackgroundColor(palette.background)
        hint.setTextColor(palette.secondaryText)
        clock.palette = palette

        credit.text = getString(R.string.credit, getString(R.string.app_name), getString(R.string.creator_name))
        credit.setTextColor(palette.accent)
        credit.setOnClickListener { openCreatorPage() }

        mediaAccess.setOnClickListener { openNotificationAccessSettings() }

        val preferences = ClockPreferences(this)
        clock.face = preferences.face
        clock.setOnClickListener { preferences.face = clock.face.toggled() }
        preferences.observeFace(lifecycle) { clock.face = it }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    // The user may have just come back from the settings screen.
    override fun onResume() {
        super.onResume()
        mediaAccess.isVisible = packageName !in NotificationManagerCompat.getEnabledListenerPackages(this)
    }

    private fun openNotificationAccessSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(this, MediaControlListener::class.java).flattenToString(),
            )
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun openCreatorPage() {
        val intent = Intent(Intent.ACTION_VIEW, getString(R.string.creator_url).toUri())
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No app on the phone can open web links, so the tap does nothing.
        }
    }
}
