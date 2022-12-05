package org.rfcx.companion.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import org.rfcx.companion.util.LocationTrackingUtils

class LocationTrackerService : Service() {

    private val binder = LocationTrackerServiceBinder()
    private val locationTrackingUtils by lazy { LocationTrackingUtils(this) }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        startTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            startTracking()
            return START_STICKY_COMPATIBILITY
        }
        startTracking()
        return START_STICKY
    }

    private fun checkAvailability(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startTracking() {
        if (!checkAvailability()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true)
            } else {
                stopSelf()
            }
        } else {
            locationTrackingUtils.createNotificationChannel()
            startForeground(
                LocationTrackingUtils.NOTIFICATION_LOCATION_ID,
                locationTrackingUtils.createLocationTrackerNotification(true)
            )
            locationTrackingUtils.startTracking()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTrackingUtils.removeListener()
        locationTrackingUtils.stopTracking()
    }

    inner class LocationTrackerServiceBinder : Binder() {
        val trackerService: LocationTrackerService
            get() = this@LocationTrackerService
    }
}
