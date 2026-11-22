package com.filipnone.dashglanceclock.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.filipnone.dashglanceclock.R

// The "<app> by FilipNone" link at the bottom of both apps' phone screens.
fun TextView.showCredit(@ColorInt accent: Int) {
    val appName = context.getString(R.string.app_name)
    val creatorName = context.getString(R.string.creator_name)
    text = context.getString(R.string.credit, appName, creatorName)
    contentDescription = context.getString(R.string.credit_description, appName, creatorName)
    setTextColor(accent)
    val gitHubMark = checkNotNull(ContextCompat.getDrawable(context, R.drawable.ic_github)).mutate()
    gitHubMark.setTint(accent)
    setCompoundDrawablesRelativeWithIntrinsicBounds(gitHubMark, null, null, null)
    setOnClickListener {
        context.tryStart(Intent(Intent.ACTION_VIEW, context.getString(R.string.creator_url).toUri()))
    }
}

// False when no app on the phone handles the intent, such as a web link with no browser.
// Callers then leave things as they were.
fun Context.tryStart(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}
