package org.rfcx.companion.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import org.rfcx.companion.util.Preferences

class LocationTrackerService : Service() {
    private val binder = LocationTrackerServiceBinder()
    private var mLocationManager: LocationManager? = null
    private var satelliteCount = 0

    override fun onCreate() {
        super.onCreate()
        if (Preferences.getInstance(this).getString(Preferences.ID_TOKEN, "").isNotEmpty()) {
            startTracker()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocationManager?.removeUpdates(locationListener)
    }


    inner class LocationTrackerServiceBinder : Binder() {
        val trackerService: LocationTrackerService
            get() = this@LocationTrackerService
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            Log.i(TAG, "onLocationChanged ${location?.longitude} , ${location?.longitude}, ${location?.altitude}")
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d(TAG, "onStatusChanged $provider $status")
        }

        override fun onProviderEnabled(provider: String?) {
            Log.i(TAG, " onProviderEnabled$provider")
        }

        override fun onProviderDisabled(provider: String?) {
            Log.i(TAG, "onProviderDisabled $provider")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG, "onBind")
        return binder
    }

    private val gnssStatusCallback = @RequiresApi(Build.VERSION_CODES.N)
    object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus?) {
            super.onSatelliteStatusChanged(status)
            val satCount = status?.satelliteCount ?: 0
            satelliteCount = satCount
        }
    }

    @Deprecated("For old version")
    @SuppressLint("MissingPermission")
    private val gpsStatusListener = GpsStatus.Listener { event ->
        if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
            var satCount: Int
            try {
                val status = mLocationManager?.getGpsStatus(null)
                val sat = status?.satellites?.iterator()
                satCount = 0
                if (sat != null) {
                    while (sat.hasNext()) {
                        satCount++
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                satCount = 0 // set min of satellite?
            }
            satelliteCount = satCount
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
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        try {
            mLocationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL,
                LOCATION_DISTANCE,
                locationListener
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager?.registerGnssStatusCallback(gnssStatusCallback)
            } else {
                mLocationManager?.addGpsStatusListener(gpsStatusListener)
            }
        } catch (ex: SecurityException) {
            ex.printStackTrace()
            Log.w(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
            Log.w(TAG, "gps provider does not exist " + ex.message)
        }

    }

    companion object {
        private const val LOCATION_INTERVAL = 1000L * 20L // 20 seconds
        private const val LOCATION_DISTANCE = 0f// 0 meter
        private const val TAG = " LocationTrackerService"
    }
}
