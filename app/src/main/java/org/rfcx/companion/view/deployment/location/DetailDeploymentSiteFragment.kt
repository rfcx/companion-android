package org.rfcx.companion.view.deployment.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.mapbox.android.core.location.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.*
import org.rfcx.companion.R
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.locate.LocationFragment
import org.rfcx.companion.view.map.MapboxCameraUtils

class DetailDeploymentSiteFragment : Fragment(), OnMapReadyCallback {

    // Mapbox
    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView
    private var symbolManager: SymbolManager? = null

    // Arguments
    var siteId: Int = 0
    var siteName: String = ""
    var isCreateNew: Boolean = false

    // Location
    private var currentUserLocation: Location? = null
    private var locationEngine: LocationEngine? = null
    private val mapboxLocationChangeCallback =
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                if (activity != null) {
                    val location = result?.lastLocation
                    location ?: return

                    currentUserLocation = location

                    if (isCreateNew) {
                        updateView()
                        currentUserLocation?.let { currentUserLocation ->
                            val latLng =
                                LatLng(currentUserLocation.latitude, currentUserLocation.longitude)
                            moveCamera(latLng, null, DEFAULT_ZOOM)
                            setCheckboxForResumeDeployment(
                                currentUserLocation.toLatLng(),
                                context?.getLastLocation()?.toLatLng() ?: LatLng()
                            )
                        }
                    }
                }
            }

            override fun onFailure(exception: Exception) {}
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { Mapbox.getInstance(it, getString(R.string.mapbox_token)) }
        initIntent()
    }

    private fun initIntent() {
        arguments?.let {
            siteId = it.getInt(ARG_SITE_ID)
            siteName = it.getString(ARG_SITE_NAME) ?: ""
            isCreateNew = it.getBoolean(ARG_IS_CREATE_NEW)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_detail_deployment_site, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        updateView()
    }

    fun updateView() {
        currentUserLocation?.let {
            altitudeValue.text = it.altitude.setFormatLabel()
            setLatLngLabel(it.toLatLng())
        }
        siteValueTextView.text = siteName
        changeGroupTextView.visibility = if (isCreateNew) View.VISIBLE else View.GONE
    }

    private fun setLatLngLabel(location: LatLng) {
        context?.let {
            val latLng =
                "${location.latitude.latitudeCoordinates(it)}, ${location.longitude.longitudeCoordinates(
                    it
                )}"
            coordinatesValueTextView.text = latLng
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.uiSettings.setAllGesturesEnabled(false)
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false
        mapboxMap.setStyle(Style.OUTDOORS) {
            setupSymbolManager(it)
            createSiteSymbol(context?.getLastLocation()?.toLatLng() ?: LatLng())
            enableLocationComponent()
        }
    }

    private fun setupSymbolManager(style: Style) {
        this.mapboxMap?.let { mapboxMap ->
            symbolManager = SymbolManager(this.mapView, mapboxMap, style)
            symbolManager?.iconAllowOverlap = true

            style.addImage(
                PROPERTY_MARKER_IMAGE,
                ResourcesCompat.getDrawable(this.resources, R.drawable.ic_pin_map, null)!!
            )
        }
    }

    private fun createSiteSymbol(latLng: LatLng) {
        symbolManager?.deleteAll()
        symbolManager?.create(
            SymbolOptions()
                .withLatLng(latLng)
                .withIconImage(PROPERTY_MARKER_IMAGE)
                .withIconSize(0.75f)
        )
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

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        if (hasPermissions()) {
            val loadedMapStyle = mapboxMap?.style
            val locationComponent = mapboxMap?.locationComponent
            // Activate the LocationComponent
            val customLocationComponentOptions = context?.let {
                LocationComponentOptions.builder(it)
                    .trackingGesturesManagement(true)
                    .accuracyColor(ContextCompat.getColor(it, R.color.colorPrimary))
                    .build()
            }

            val locationComponentActivationOptions =
                context?.let {
                    LocationComponentActivationOptions.builder(it, loadedMapStyle!!)
                        .locationComponentOptions(customLocationComponentOptions)
                        .build()
                }

            mapboxMap?.let { it ->
                it.locationComponent.apply {
                    if (locationComponentActivationOptions != null) {
                        activateLocationComponent(locationComponentActivationOptions)
                    }

                    isLocationComponentEnabled = true
                    renderMode = RenderMode.COMPASS
                }
            }

            this.currentUserLocation = locationComponent?.lastKnownLocation
            initLocationEngine()
        } else {
            requestPermissions()
        }
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

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
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

    private fun moveCamera(userPosition: LatLng, nearestSite: LatLng?, zoom: Double) {
        mapboxMap?.moveCamera(
            MapboxCameraUtils.calculateLatLngForZoom(
                userPosition,
                nearestSite,
                zoom
            )
        )
    }

    private fun setCheckboxForResumeDeployment(curLoc: LatLng, target: LatLng) {
        val distance = curLoc.distanceTo(target)
        if (distance <= 20) {
            setWithinText()
        } else {
            setNotWithinText(distance.setFormatLabel())
        }
    }

    private fun setWithinText() {
        withinTextView.text = getString(R.string.within)
        withinTextView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_checklist_passed,
            0,
            0,
            0
        )
    }

    private fun setNotWithinText(distance: String) {
        withinTextView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_checklist_cross,
            0,
            0,
            0
        )
        withinTextView.text = getString(R.string.more_than, distance)
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
        private const val ARG_SITE_ID = "ARG_SITE_ID"
        private const val ARG_SITE_NAME = "ARG_SITE_NAME"
        private const val ARG_IS_CREATE_NEW = "ARG_IS_CREATE_NEW"

        const val PROPERTY_MARKER_IMAGE = "marker.image"
        const val DEFAULT_ZOOM = 15.0

        @JvmStatic
        fun newInstance(id: Int, name: String?, isCreateNew: Boolean = false) =
            DetailDeploymentSiteFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SITE_ID, id)
                    putString(ARG_SITE_NAME, name)
                    putBoolean(ARG_IS_CREATE_NEW, isCreateNew)
                }
            }
    }
}
