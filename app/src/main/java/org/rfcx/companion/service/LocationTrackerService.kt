package org.rfcx.companion.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import org.rfcx.companion.MainActivity
import org.rfcx.companion.R
import org.rfcx.companion.util.Preferences

class LocationTrackerService : Service() {
    private val binder = LocationTrackerServiceBinder()
    private var mLocationManager: LocationManager? = null
    private var isLocationAvailability: Boolean = true

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            Log.i(
                TAG,
                "onLocationChanged ${location?.longitude}, ${location?.longitude}, ${location?.altitude}"
            )
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String?) {}

        override fun onProviderDisabled(provider: String?) {}
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        if (Preferences.getInstance(this).getString(Preferences.ID_TOKEN, "").isNotEmpty()) {
            startTracker()
        }
    }

    private fun startTracker() {
        val check = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!check) {
            this.stopSelf()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        try {
            mLocationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL,
                LOCATION_DISTANCE,
                locationListener
            )
            startForeground(NOTIFICATION_LOCATION_ID, createLocationTrackerNotification(true))
        } catch (ex: SecurityException) {
            ex.printStackTrace()
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
    }

    private fun createLocationTrackerNotification(isLocationAvailability: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, NOTIFICATION_LOCATION_CHANNEL_ID).apply {
            setContentTitle(getString(R.string.tracking_enabled))
            this@LocationTrackerService.isLocationAvailability = isLocationAvailability
            if (!this@LocationTrackerService.isLocationAvailability) {
                setContentText(getString(R.string.location_setting))
            }
            setSmallIcon(R.drawable.ic_notification)
            setOnlyAlertOnce(true)
            setContentIntent(pendingIntent)
            priority = NotificationCompat.PRIORITY_HIGH
        }.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_LOCATION_CHANNEL_ID,
            NOTIFICATION_LOCATION_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(false)
            enableLights(false)
            setSound(null, null)
            setShowBadge(false)
        }
        getNotificationManager().createNotificationChannel(channel)
    }

    private fun getNotificationManager(): NotificationManager {
        return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocationManager?.removeUpdates(locationListener)
    }

    inner class LocationTrackerServiceBinder : Binder() {
        val trackerService: LocationTrackerService
            get() = this@LocationTrackerService
    }

    companion object {
        const val NOTIFICATION_LOCATION_ID = 22
        const val NOTIFICATION_LOCATION_NAME = "Track location"
        const val NOTIFICATION_LOCATION_CHANNEL_ID = "Location"
        private const val LOCATION_INTERVAL = 1000L * 20L // 20 seconds
        private const val LOCATION_DISTANCE = 0f// 0 meter
        private const val TAG = "LocationTrackerService"
    }
}
