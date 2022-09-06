package org.rfcx.companion.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import io.realm.Realm
import org.rfcx.companion.MainActivity
import org.rfcx.companion.R
import org.rfcx.companion.entity.Coordinate
import org.rfcx.companion.entity.Tracking
import org.rfcx.companion.localdb.TrackingDb
import org.rfcx.companion.util.*
import org.rfcx.companion.util.Preferences.Companion.LASTEST_GET_LOCATION_TIME
import java.util.*
import kotlin.concurrent.fixedRateTimer

class LocationTrackerService : Service() {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val trackingDb by lazy { TrackingDb(realm) }

    private val binder = LocationTrackerServiceBinder()
    private var mLocationManager: LocationManager? = null
    private var isLocationAvailability: Boolean = true
    private var tracking = Tracking()

    private var trackingStatTimer: Timer? = null
    private val handler: Handler = Handler()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            saveLocation(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                if ((System.currentTimeMillis() - Preferences.getInstance(this@LocationTrackerService).getLong(LASTEST_GET_LOCATION_TIME, 0L)) > 10 * 1000L) {
                    getNotificationManager().notify(NOTIFICATION_LOCATION_ID, createLocationTrackerNotification(true))
                }
            } else if (status == LocationProvider.OUT_OF_SERVICE) {
                getNotificationManager().notify(NOTIFICATION_LOCATION_ID, createLocationTrackerNotification(false))
            }
        }

        override fun onProviderEnabled(provider: String) {
            getNotificationManager().notify(NOTIFICATION_LOCATION_ID, createLocationTrackerNotification(true))
        }

        override fun onProviderDisabled(provider: String) {
            getNotificationManager().notify(NOTIFICATION_LOCATION_ID, createLocationTrackerNotification(false))
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        if (Preferences.getInstance(this).getString(Preferences.ID_TOKEN, "").isNotEmpty()) {
            startTracker()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            if (Preferences.getInstance(this).getString(Preferences.ID_TOKEN, "").isNotEmpty()) {
                startTracker()
            }
            return START_STICKY_COMPATIBILITY
        }
        if (Preferences.getInstance(this).getString(Preferences.ID_TOKEN, "").isNotEmpty()) {
            startTracker()
        }
        return START_STICKY
    }

    private fun saveLocation(location: Location) {
        tracking.id = 1
        val coordinate = Coordinate(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude
        )
        trackingDb.insertOrUpdate(tracking, coordinate)
        Preferences.getInstance(this).putLong(LASTEST_GET_LOCATION_TIME, System.currentTimeMillis())
    }

    private fun startTracker() {
        val check = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!check) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true)
            } else {
                stopSelf()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        startForeground(NOTIFICATION_LOCATION_ID, createLocationTrackerNotification(true))
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        try {
            mLocationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL,
                LOCATION_DISTANCE,
                locationListener
            )

            trackingStatTimer?.cancel()
            trackingStatTimer = fixedRateTimer("timer", false, 60 * 1000L, 60 * 1000L) {
                getNotificationManager().notify(
                    NOTIFICATION_LOCATION_ID,
                    createLocationTrackerNotification(isLocationAvailability)
                )
            }
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
            intent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_LOCATION_CHANNEL_ID).apply {
            setContentTitle(getString(R.string.tracking_enabled))
            this@LocationTrackerService.isLocationAvailability = isLocationAvailability
            if (this@LocationTrackerService.isLocationAvailability) {
                setContentText(
                    getString(
                        R.string.notification_traking_message_format,
                        LocationTracking.getOnDutyTimeMinute(this@LocationTrackerService)
                    )
                )
            } else {
                setContentText(getString(R.string.notification_location_not_availability))
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
        trackingStatTimer?.cancel()
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
        private const val LOCATION_DISTANCE = 0f // 0 meter
        private const val TAG = "LocationTrackerService"
    }
}
