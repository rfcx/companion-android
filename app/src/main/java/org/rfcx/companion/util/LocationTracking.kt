package org.rfcx.companion.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mapbox.mapboxsdk.geometry.LatLng
import org.rfcx.companion.localdb.TrackingDb
import org.rfcx.companion.service.LocationTrackerService

class LocationTracking {
    companion object {

        private const val TRACKING_ON = true
        private const val TRACKING_OFF = false
        private const val MILLI_SECS_PER_MINUTE = (60 * 1000).toLong()

        fun isTrackingOn(context: Context): Boolean {
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
            updateService(context)
        }

        private fun updateService(context: Context) {
            if (isTrackingOn(context)) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, LocationTrackerService::class.java)
                )
                startDutyTracking(context)
            } else {
                context.stopService(Intent(context, LocationTrackerService::class.java))
                stopDutyTracking(context)
            }
        }

        private fun startDutyTracking(context: Context) {
            val preferences = Preferences.getInstance(context)
            if (preferences.getLong(Preferences.ON_DUTY_LAST_OPEN, 0L) == 0L) {
                preferences.putLong(Preferences.ON_DUTY_LAST_OPEN, System.currentTimeMillis())
            }
        }

        private fun stopDutyTracking(context: Context) {
            val preferences = Preferences.getInstance(context)
            val lastOpen: Long = preferences.getLong(Preferences.ON_DUTY_LAST_OPEN, 0L)
            val stopTime = System.currentTimeMillis()
            preferences.putLong(Preferences.ON_DUTY_LAST_OPEN, 0L)

            if (lastOpen != 0L && stopTime > lastOpen) {
                val onDutyMinute = ((stopTime - lastOpen) / MILLI_SECS_PER_MINUTE).toInt()
                adjustOnDuty(context, onDutyMinute)
            }

        }

        private fun adjustOnDuty(context: Context, minutes: Int) {
            val preferences = Preferences.getInstance(context)
            var lastedDuty: Long = preferences.getLong(Preferences.ON_DUTY, 0L)
            lastedDuty += minutes
            preferences.putLong(Preferences.ON_DUTY, lastedDuty)
        }

        fun getOnDutyTimeMinute(context: Context): Long {
            val preferences = Preferences.getInstance(context)
            val lastOnDutyTime = preferences.getLong(Preferences.ON_DUTY, 0L)
            val lastDutyOpenTime = preferences.getLong(Preferences.ON_DUTY_LAST_OPEN, 0L)

            return if (lastDutyOpenTime != 0L) {
                val currentTime = System.currentTimeMillis()
                val difTime = currentTime - lastDutyOpenTime
                val onDutyNow = difTime / MILLI_SECS_PER_MINUTE
                lastOnDutyTime + onDutyNow
            } else {
                preferences.getLong(Preferences.ON_DUTY, 0L)
            }
        }

        fun getDistance(trackingDb: TrackingDb): Double {
            var distance = 0.0
            trackingDb.getFirstTracking()?.let { tracking ->
                tracking.points.forEachIndexed { index, element ->
                    if (index != 0 && tracking.points[index - 1] != null) {
                        val latLng = LatLng(
                            tracking.points[index - 1]?.latitude ?: 0.0,
                            tracking.points[index - 1]?.longitude ?: 0.0
                        )
                        distance += LatLng(element.latitude, element.longitude).distanceTo(latLng)
                    }
                }
            }
            return distance
        }
    }
}
