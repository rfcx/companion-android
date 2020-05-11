package org.rfcx.audiomoth.view.configure


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.audiomoth.MainActivity
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Device
import org.rfcx.audiomoth.entity.LatLong
import org.rfcx.audiomoth.entity.Stream
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICES
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICE_ID
import java.sql.Timestamp

class DeployFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapView: MapView
    private lateinit var symbolManager: SymbolManager
    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deploy, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { Mapbox.getInstance(it, MAPBOX_ACCESS_TOKEN) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        getLastLocation()

        finishButton.setOnClickListener {
            saveDevice()
        }
    }

    private fun saveDevice() {
        var deviceId = ""
        var batteryLevel = 0
        var datePredictTimeMillis: Long = 0
        if (arguments?.containsKey(DEVICE_ID) == true && arguments?.containsKey(
                DATE_PREDICT_TIME_MILLIS
            ) == true && arguments?.containsKey(
                BATTERY_LEVEL
            ) == true
        ) {
            arguments?.let {
                deviceId = it.getString(DEVICE_ID).toString()
                batteryLevel = it.getInt(BATTERY_LEVEL)
                datePredictTimeMillis = it.getLong(DATE_PREDICT_TIME_MILLIS)
            }
        }

        // TODO: Update later it is mockup!
        val latLong = LatLong(
            latitudeEditText.text.toString().toDouble(),
            longitudeEditText.text.toString().toDouble()
        )
        val stream =
            Stream("stream01", 3, 8, false, 5, 10, arrayListOf(), ConfigureFragment.RECOMMENDED)
        val device = Device(
            deviceId,
            "",
            "",
            Timestamp(System.currentTimeMillis()),
            latLong,
            locationNameEditText.text.toString(),
            batteryLevel,
            Timestamp(datePredictTimeMillis),
            stream
        )
        Firestore().db.collection(DEVICES).document().set(device)
            .addOnCompleteListener {
                context?.let { it1 -> MainActivity.startActivity(it1, true) }
            }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.OUTDOORS) {
            symbolManager = SymbolManager(mapView, mapboxMap, it)
            symbolManager.iconAllowOverlap = true
            symbolManager.iconIgnorePlacement = true

            lastLocation?.let { lastLocation ->
                setPinOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                setupView(lastLocation.latitude.toString(), lastLocation.longitude.toString())
            }

            onLatLngChanged()
        }
    }

    private fun setPinOnMap(latLng: LatLng) {
        symbolManager.deleteAll()

        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map, null)
        val mBitmap = BitmapUtils.getBitmapFromDrawable(drawable)
        if (mBitmap != null) {
            mapboxMap.style?.addImage(PIN_MAP, mBitmap)
        }

        symbolManager.create(
            SymbolOptions()
                .withLatLng(latLng)
                .withIconImage(PIN_MAP)
                .withIconSize(1.0f)
        )

        mapboxMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng,
                15.0
            )
        )
    }

    private fun onLatLngChanged() {
        latitudeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null) {
                    if (p0.toString() != "-" && p0.isNotEmpty()) {
                        if (p0.toString().toDouble() >= -90.0 && p0.toString().toDouble() <= 90) {
                            setPinOnMap(
                                LatLng(
                                    p0.toString().toDouble(),
                                    longitudeEditText.text.toString().toDouble()
                                )
                            )
                        }
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        longitudeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null) {
                    if (p0.toString() != "-" && p0.isNotEmpty()) {
                        setPinOnMap(
                            LatLng(
                                latitudeEditText.text.toString().toDouble(),
                                p0.toString().toDouble()
                            )
                        )
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private val locationListener = object : android.location.LocationListener {
        override fun onLocationChanged(p0: Location?) {}
        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
        override fun onProviderEnabled(p0: String?) {}
        override fun onProviderDisabled(p0: String?) {}
    }

    private fun getLastLocation() {
        if (checkPermissions()) {
            locationManager?.removeUpdates(locationListener)
            locationManager =
                activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

            try {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5 * 1000L,
                    0f,
                    locationListener
                )
                lastLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            } catch (ex: SecurityException) {
                ex.printStackTrace()
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace()
            }
        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissionState = context?.let {
            ActivityCompat.checkSelfPermission(
                it,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity?.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        } else {
            throw Exception("Request permissions not required before API 23 (should never happen)")
        }
    }

    private fun setupView(latitudeText: String, longitudeText: String) {
        latitudeEditText.setText(latitudeText)
        longitudeEditText.setText(longitudeText)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    companion object {
        const val TAG = "DeployFragment"
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        const val PIN_MAP = "pin-map"
        const val MAPBOX_ACCESS_TOKEN =
            "pk.eyJ1IjoicmF0cmVlLW9jaG4iLCJhIjoiY2s5Mjk5MDQ3MDYzcDNmbzVnZHd1aXNqaSJ9.UCrMjgGw8zROm_sRlebSGQ"

        private const val DATE_PREDICT_TIME_MILLIS = "datePredictTimeMillis"
        private const val BATTERY_LEVEL = "batteryLevel"

        fun newInstance(
            deviceId: String,
            batteryLv: Int,
            datePredictTimeMillis: Long
        ): DeployFragment {
            return DeployFragment().apply {
                arguments = Bundle().apply {
                    putString(DEVICE_ID, deviceId)
                    putLong(DATE_PREDICT_TIME_MILLIS, datePredictTimeMillis)
                    putInt(BATTERY_LEVEL, batteryLv)
                }
            }
        }
    }
}
