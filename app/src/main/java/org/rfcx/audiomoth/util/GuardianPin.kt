package org.rfcx.audiomoth.util

import android.content.Context
import android.util.Log

object GuardianPin {
    const val CONNECTED_GUARDIAN = "GUARDIAN_CONNECTED"
    const val NOT_CONNECTED_GUARDIAN = "GUARDIAN_NOT_CONNECTED"

    fun getGuardianPinImage(context: Context, wifiName: String): String {
        val currentWifiName = WifiHotspotUtils.getCurrentWifiName(context)
        return if (currentWifiName == wifiName) {
            CONNECTED_GUARDIAN
        } else {
            NOT_CONNECTED_GUARDIAN
        }
    }
}
