package org.rfcx.companion.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.realm.Realm
import org.rfcx.companion.MainActivity
import org.rfcx.companion.R
import org.rfcx.companion.entity.Coordinate
import org.rfcx.companion.entity.Tracking
import org.rfcx.companion.localdb.TrackingDb
import java.util.*
import kotlin.concurrent.fixedRateTimer

class LocationTrackingUtils(private val context: Context) {

    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val trackingDb by lazy { TrackingDb(realm) }

    private var isLocationAvailability: Boolean = true
    private var mLocationManager: LocationManager? = null
    private var trackingStatTimer: Timer? = null
    private var tracking = Tracking()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            saveLocation(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                if ((System.currentTimeMillis() - Preferences.getInstance(context).getLong(
                        Preferences.LASTEST_GET_LOCATION_TIME, 0L
                    )) > 10 * 1000L
                ) {
                    getNotificationManager().notify(
                        NOTIFICATION_LOCATION_ID,
                        createLocationTrackerNotification(true)
                    )
                }
            } else if (status == LocationProvider.OUT_OF_SERVICE) {
                getNotificationManager().notify(
                    NOTIFICATION_LOCATION_ID,
                    createLocationTrackerNotification(false)
                )
            }
        }

        override fun onProviderEnabled(provider: String) {
            getNotificationManager().notify(
                NOTIFICATION_LOCATION_ID,
                createLocationTrackerNotification(true)
            )
        }

        override fun onProviderDisabled(provider: String) {
            getNotificationManager().notify(
                NOTIFICATION_LOCATION_ID,
                createLocationTrackerNotification(false)
            )
        }
    }

    companion object {
        const val NOTIFICATION_LOCATION_ID = 22
        const val NOTIFICATION_LOCATION_NAME = "Track location"
        const val NOTIFICATION_LOCATION_CHANNEL_ID = "Location"
        private const val LOCATION_INTERVAL = 1000L * 20L // 20 seconds
        private const val LOCATION_DISTANCE = 0f // 0 meter
    }

    fun startTracking() {
        if (Preferences.getInstance(context).getString(Preferences.ID_TOKEN, "").isEmpty()) return

        mLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
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

    private fun saveLocation(location: Location) {
        tracking.id = 1
        val coordinate = Coordinate(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude
        )
        try {
            trackingDb.insertOrUpdate(tracking, coordinate)
        } catch (e: IllegalStateException) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        Preferences.getInstance(context)
            .putLong(Preferences.LASTEST_GET_LOCATION_TIME, System.currentTimeMillis())
    }

    private fun getNotificationManager(): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
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
    }

    fun createLocationTrackerNotification(isLocationAvailability: Boolean): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            intent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(
            context,
            NOTIFICATION_LOCATION_CHANNEL_ID
        ).apply {
            setContentTitle(context.getString(R.string.tracking_enabled))
            this@LocationTrackingUtils.isLocationAvailability = isLocationAvailability
            if (this@LocationTrackingUtils.isLocationAvailability) {
                setContentText(
                    context.getString(
                        R.string.notification_traking_message_format,
                        LocationTrackingManager.getOnDutyTimeMinute(context)
                    )
                )
            } else {
                setContentText(context.getString(R.string.notification_location_not_availability))
            }
            setSmallIcon(R.drawable.ic_notification)
            setOnlyAlertOnce(true)
            setContentIntent(pendingIntent)
            priority = NotificationCompat.PRIORITY_HIGH
        }.build()
    }

    fun removeListener() {
        mLocationManager?.removeUpdates(locationListener)
    }

    fun stopTracking() {
        trackingStatTimer?.cancel()
    }
}
