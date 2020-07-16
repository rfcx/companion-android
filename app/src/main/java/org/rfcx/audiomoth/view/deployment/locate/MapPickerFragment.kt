package org.rfcx.audiomoth.view.deployment.locate

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.mapbox.android.core.location.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.fragment_map_picker.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.latitudeCoordinates
import org.rfcx.audiomoth.util.longitudeCoordinates
import org.rfcx.audiomoth.view.deployment.DeploymentProtocol
import org.rfcx.audiomoth.view.deployment.locate.LocationFragment.Companion.DEFAULT_ZOOM

class MapPickerFragment : Fragment(), OnMapReadyCallback {
    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView
    private var deploymentProtocol: DeploymentProtocol? = null
    private var locationEngine: LocationEngine? = null
    private var currentUserLocation: Location? = null
    private var selectedLocation: Location? = null

    private val mapboxLocationChangeCallback =
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                if (activity != null) {
                    val location = result?.lastLocation
                    location ?: return

                    mapboxMap?.let {
                        this@MapPickerFragment.currentUserLocation = location
                    }

                    if (selectedLocation == null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        moveCamera(latLng, DEFAULT_ZOOM)
                        setLatLogLabel(latLng)
                    }
                }
            }

            override fun onFailure(exception: Exception) {
                Log.e(LocationFragment.TAG, exception.localizedMessage ?: "empty localizedMessage")
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as DeploymentProtocol
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_picker, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { Mapbox.getInstance(it, getString(R.string.mapbox_token)) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapBoxPickerView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        enableLocationComponent()

        selectButton.setOnClickListener {
            val currentCameraPosition = mapboxMap?.cameraPosition?.target
            currentCameraPosition?.let {
                deploymentProtocol?.startLocation(it.latitude, it.longitude)
            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false
        mapboxMap.setStyle(Style.OUTDOORS)

        mapboxMap.addOnCameraMoveListener {
            val currentCameraPosition = mapboxMap.cameraPosition.target
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = currentCameraPosition.latitude
            loc.longitude = currentCameraPosition.longitude
            selectedLocation = loc
            setLatLogLabel(LatLng(currentCameraPosition.latitude, currentCameraPosition.longitude))
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Double) {
        mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        if (hasPermissions()) {
            val loadedMapStyle = mapboxMap?.style
            val locationComponent = mapboxMap?.locationComponent
            context?.let {
                locationComponent?.activateLocationComponent(
                    LocationComponentActivationOptions.builder(it, loadedMapStyle!!)
                        .useDefaultLocationEngine(false)
                        .build()
                )
            }
            locationComponent?.isLocationComponentEnabled = false
            locationComponent?.renderMode = RenderMode.COMPASS

            val lastKnownLocation = locationComponent?.lastKnownLocation
            lastKnownLocation?.let {
                this.currentUserLocation = it
                moveCamera(LatLng(it.latitude, it.longitude), DEFAULT_ZOOM)
            }
            initLocationEngine()
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        locationEngine = context?.let { LocationEngineProvider.getBestLocationEngine(it) }
        val request =
            LocationEngineRequest.Builder(LocationFragment.DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(LocationFragment.DEFAULT_MAX_WAIT_TIME).build()

        locationEngine?.requestLocationUpdates(
            request,
            mapboxLocationChangeCallback,
            Looper.getMainLooper()
        )

        locationEngine?.getLastLocation(mapboxLocationChangeCallback)
    }

    private fun hasPermissions(): Boolean {
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
                LocationFragment.REQUEST_PERMISSIONS_REQUEST_CODE
            )
        } else {
            throw Exception("Request permissions not required before API 23 (should never happen)")
        }
    }

    private fun setLatLogLabel(location: LatLng) {
        context?.let {
            val latLng =
                "${location.latitude.latitudeCoordinates(it)}, ${location.longitude.longitudeCoordinates(
                    it
                )}"
            locationTextView.text = latLng
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LocationFragment.REQUEST_PERMISSIONS_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableLocationComponent()
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    companion object {
        @JvmStatic
        fun newInstance(): MapPickerFragment {
            return MapPickerFragment()
        }
    }
}
