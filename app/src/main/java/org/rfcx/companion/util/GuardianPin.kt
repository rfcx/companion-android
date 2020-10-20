package org.rfcx.companion.util

import android.content.Context

object GuardianPin {
    const val CONNECTED_GUARDIAN = "GUARDIAN_CONNECTED"
    const val NOT_CONNECTED_GUARDIAN = "GUARDIAN_NOT_CONNECTED"

    fun getGuardianPinImage(context: Context, wifiName: String): String {
        return if (WifiHotspotUtils.isConnectedWithGuardian(context, wifiName)) {
            CONNECTED_GUARDIAN
        } else {
            NOT_CONNECTED_GUARDIAN
        }
    }
}
