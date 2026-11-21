package com.filipnone.dashglanceclock

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.filipnone.dashglanceclock.clock.ClockFace
import com.filipnone.dashglanceclock.clock.ClockPalette
import com.filipnone.dashglanceclock.clock.ClockPreferences
import com.filipnone.dashglanceclock.media.MediaControlListener
import com.filipnone.dashglanceclock.media.MusicControls
import com.filipnone.dashglanceclock.ui.ClockView
import com.filipnone.dashglanceclock.ui.DesignPicker
import com.filipnone.dashglanceclock.ui.DesignSheet
import com.filipnone.dashglanceclock.ui.SheetHandle

class MainActivity : ComponentActivity() {

    private lateinit var mediaAccess: CompoundButton
    private lateinit var musicControls: MusicControls

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root)
        val content = findViewById<View>(R.id.content)
        val clock = findViewById<ClockView>(R.id.clock)
        val hint = findViewById<TextView>(R.id.hint)
        val panel = findViewById<View>(R.id.design_panel)
        val designTitle = findViewById<TextView>(R.id.design_title)
        val designs = findViewById<DesignPicker>(R.id.designs)
        val footer = findViewById<View>(R.id.footer)
        val credit = findViewById<TextView>(R.id.credit)
        mediaAccess = findViewById(R.id.media_access)
        musicControls = MusicControls(this)
        // Only the portrait layout has the sheet's header and handle.
        val sheetHeader: View? = findViewById(R.id.design_sheet_header)
        val sheetHandle: SheetHandle? = findViewById(R.id.design_sheet_handle)

        val isDarkMode = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val palette = ClockPalette.forDarkMode(isDarkMode)
        val density = resources.displayMetrics.density
        root.setBackgroundColor(palette.background)
        hint.setTextColor(palette.secondaryText)
        designTitle.setTextColor(palette.primaryText)

        val corner = PANEL_CORNER_DP * density
        panel.background = GradientDrawable().apply {
            setColor(palette.dial)
            if (sheetHeader != null) {
                cornerRadii = floatArrayOf(corner, corner, corner, corner, 0f, 0f, 0f, 0f)
            } else {
                // A single radius, not an array, so the outline stays a rounded rectangle
                // that clipToOutline can clip to on every Android version.
                cornerRadius = corner
            }
        }
        val designSheet = if (sheetHeader != null && sheetHandle != null) {
            sheetHandle.color = ColorUtils.setAlphaComponent(palette.secondaryText, HANDLE_ALPHA)
            footer.setBackgroundColor(palette.dial)
            DesignSheet(this, panel, sheetHeader, sheetHandle, designs, content, clock)
        } else {
            // The side panel's rows scroll under its rounded corners, so it clips them.
            panel.clipToOutline = true
            ViewCompat.setAccessibilityHeading(designTitle, true)
            null
        }

        val appName = getString(R.string.app_name)
        val creatorName = getString(R.string.creator_name)
        credit.text = getString(R.string.credit, appName, creatorName)
        credit.contentDescription = getString(R.string.credit_description, appName, creatorName)
        credit.setTextColor(palette.accent)
        val gitHubMark = checkNotNull(ContextCompat.getDrawable(this, R.drawable.ic_github)).mutate()
        gitHubMark.setTint(palette.accent)
        credit.setCompoundDrawablesRelativeWithIntrinsicBounds(gitHubMark, null, null, null)
        credit.setOnClickListener { openCreatorPage() }

        // Turning the switch off only turns music controls off in the app; the notification
        // access stays granted. Turning it on opens Android's settings only if access was never
        // granted or has been revoked there, and onResume then shows what the user chose.
        // Setting the switch to the current state doesn't count as a flip.
        mediaAccess.setTextColor(palette.primaryText)
        mediaAccess.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == musicControls.isOn) return@setOnCheckedChangeListener
            musicControls.isEnabled = isChecked
            if (isChecked && !musicControls.hasAccess) openNotificationAccessSettings()
        }

        val preferences = ClockPreferences(this)
        // Switching faces, from the phone or the car, makes the sheet hop again. Picking a
        // design and the first call here don't.
        var shownFace: ClockFace? = null
        fun showChosenDesign() {
            val face = preferences.face
            if (shownFace != null && face != shownFace) designSheet?.hint()
            shownFace = face
            clock.design = preferences.designFor(face)
            designTitle.setText(
                when (face) {
                    ClockFace.ANALOG -> R.string.design_title_analog
                    ClockFace.DIGITAL -> R.string.design_title_digital
                },
            )
            designs.show(face, clock.design, palette)
        }
        showChosenDesign()
        clock.setOnClickListener { preferences.face = preferences.face.toggled() }
        designs.onPick = { preferences.select(it) }
        preferences.observe(lifecycle) { showChosenDesign() }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            if (sheetHeader != null) {
                view.updatePadding(left = bars.left, top = bars.top, right = bars.right)
                // The footer shares the sheet's color and fills in behind the navigation bar,
                // so the sheet looks like it runs to the bottom of the screen.
                footer.updatePadding(bottom = bars.bottom)
            } else {
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            }
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

    // No app on the phone can open web links if this fails, so the tap does nothing.
    private fun openCreatorPage() {
        tryStart(Intent(Intent.ACTION_VIEW, getString(R.string.creator_url).toUri()))
    }

    private fun tryStart(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private companion object {
        const val PANEL_CORNER_DP = 24f
        const val HANDLE_ALPHA = 0x99
    }
}
