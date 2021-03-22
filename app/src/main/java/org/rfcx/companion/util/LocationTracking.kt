package org.rfcx.companion.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import org.rfcx.companion.service.LocationTrackerService

class LocationTracking {
    companion object {

        private const val TRACKING_ON = true
        private const val TRACKING_OFF = false

        private fun isOn(context: Context): Boolean {
            val preferences = Preferences.getInstance(context)
            val state = preferences.getBoolean(Preferences.ENABLE_LOCATION_TRACKING, TRACKING_OFF)
            return state == TRACKING_ON
        }

        fun set(context: Context, on: Boolean) {
            val preferences = Preferences.getInstance(context)
            preferences.putBoolean(
                Preferences.ENABLE_LOCATION_TRACKING,
                if (on) TRACKING_ON else TRACKING_OFF
            )
            Log.i("LocationTrackerService", "on $on")
            updateService(context)
        }

        private fun updateService(context: Context) {
            if (isOn(context)) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, LocationTrackerService::class.java)
                )
            } else {
                context.stopService(Intent(context, LocationTrackerService::class.java))
            }
        }
    }
}
