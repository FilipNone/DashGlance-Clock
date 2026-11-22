package com.filipnone.dashglanceclock.car

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator
import com.filipnone.dashglanceclock.R

class ClockCarAppService : CarAppService() {

    // Debug builds trust any host, which keeps testing simple. Release builds only trust the
    // Google hosts listed in hosts_allowlist.xml.
    override fun createHostValidator(): HostValidator =
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(R.array.hosts_allowlist)
                .build()
        }

    override fun onCreateSession(sessionInfo: SessionInfo): Session = ClockSession()
}
