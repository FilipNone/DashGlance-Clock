package com.filipnone.dashglanceclock.media

import android.service.notification.NotificationListenerService

// Its only job is to exist: Android hands other apps' media sessions to apps that have a
// notification listener the user approved. While music controls are off in the app, it asks
// Android to unbind it, so DashGlance stops receiving notifications and its process isn't kept
// running for the listener. MusicControls asks for it back when they're turned on.
class MediaControlListener : NotificationListenerService() {

    private val controls by lazy { MusicControls(this) }

    override fun onListenerConnected() {
        if (!controls.isEnabled) {
            requestUnbind()
            return
        }
        controls.observe { if (!controls.isEnabled) requestUnbind() }
    }

    override fun onListenerDisconnected() = controls.stopObserving()

    override fun onDestroy() {
        controls.stopObserving()
        super.onDestroy()
    }
}
