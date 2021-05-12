package org.rfcx.companion.view.deployment.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.LocationGroup
import org.rfcx.companion.entity.LocationGroups
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol
import org.rfcx.companion.view.deployment.locate.LocationFragment
import org.rfcx.companion.view.map.MapboxCameraUtils
import org.rfcx.companion.view.profile.locationgroup.LocationGroupActivity

class DetailDeploymentSiteFragment : Fragment(), OnMapReadyCallback {
    private val analytics by lazy { context?.let { Analytics(it) } }
    private val preferences by lazy { context?.let { Preferences.getInstance(it) } }
    private var deploymentProtocol: BaseDeploymentProtocol? = null

    // Mapbox
    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView
    private var symbolManager: SymbolManager? = null

    // Arguments
    var siteId: Int = 0
    var siteName: String = ""
    var isCreateNew: Boolean = false
    var site: Locate? = null

    // Local database
    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val siteDb by lazy { LocateDb(realm) }

    // Location
    private var group: String? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    private var currentUserLocation: Location? = null
    private var userLocation: Location? = null
    private var locationEngine: LocationEngine? = null
    private val mapboxLocationChangeCallback =
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                if (activity != null) {
                    val location = result?.lastLocation
                    location ?: return

                    currentUserLocation = location
                    updateView()

                    if (isCreateNew) {
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
            latitude = it.getDouble(ARG_LATITUDE)
            longitude = it.getDouble(ARG_LONGITUDE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_detail_deployment_site, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as BaseDeploymentProtocol
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTopBar()

        mapView = view.findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        updateView()

        changeProjectTextView.setOnClickListener {
            val group = locationGroupValueTextView.text.toString()
            val setLocationGroup = if (group == getString(R.string.none)) null else group
            context?.let { it1 ->
                LocationGroupActivity.startActivity(
                    it1,
                    setLocationGroup,
                    Screen.DETAIL_DEPLOYMENT_SITE.id
                )
                analytics?.trackChangeLocationGroupEvent(Screen.DETAIL_DEPLOYMENT_SITE.id)
            }
        }

        nextButton.setOnClickListener {
            analytics?.trackSaveLocationEvent(Screen.LOCATION.id)
            this.altitude = currentUserLocation?.altitude ?: 0.0
            getLastLocation()
            if (isCreateNew) {
                createSite()
            } else {
                handleExistLocate()
            }
        }

        currentLocate.setOnClickListener {
            if (isCreateNew) {
                updateLocationOfNewSite()
            } else {
                site?.let {
                    updateLocationOfExistingSite(
                        currentUserLocation?.latitude ?: it.latitude,
                        currentUserLocation?.longitude ?: it.longitude,
                        currentUserLocation?.altitude ?: it.altitude
                    )
                }
            }
        }

        viewMapBox.setOnClickListener {
            deploymentProtocol?.let {
                if (isCreateNew) {
                    val currentLocation = currentUserLocation
                    it.startMapPicker(
                        currentLocation?.latitude ?: 0.0,
                        currentLocation?.longitude ?: 0.0,
                        -1,
                        siteName
                    )
                } else {
                    site?.let { site ->
                        it.startMapPicker(site.latitude, site.longitude, site.id, site.name)
                    }
                }
                it.hideToolbar()
            }
        }
    }

    private fun setupTopBar() {
        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }
    }

    private fun updateLocationOfNewSite() {
        latitude = 0.0
        longitude = 0.0
        val currentLatLng =
            LatLng(currentUserLocation?.latitude ?: 0.0, currentUserLocation?.longitude ?: 0.0)
        createSiteSymbol(currentLatLng)
        moveCamera(LatLng(currentLatLng), DEFAULT_ZOOM)
        setWithinText()
    }

    private fun updateLocationOfExistingSite(
        latitude: Double,
        longitude: Double,
        altitude: Double
    ) {
        var locate = Locate()
        site?.let {
            locate = Locate(
                it.id,
                it.serverId,
                getLocationGroup(it.locationGroup?.name ?: getString(R.string.none)),
                it.name,
                latitude,
                longitude,
                altitude,
                it.createdAt,
                it.updatedAt,
                it.lastDeploymentId,
                it.lastDeploymentServerId,
                it.lastGuardianDeploymentId,
                it.lastGuardianDeploymentServerId,
                it.syncState
            )
            createSiteSymbol(locate.getLatLng())
            moveCamera(LatLng(locate.getLatLng()), DEFAULT_ZOOM)
        }
        site = locate
        val currentLatLng =
            LatLng(currentUserLocation?.latitude ?: 0.0, currentUserLocation?.longitude ?: 0.0)
        setCheckboxForResumeDeployment(currentLatLng, LatLng(latitude, longitude))
    }

    private fun createSite() {
        val name = siteValueTextView.text.toString()
        userLocation?.let {
            val locate = Locate(
                name = name,
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = altitude,
                locationGroup = getLocationGroup(group ?: getString(R.string.none))
            )
            deploymentProtocol?.setDeployLocation(locate, false)
            deploymentProtocol?.nextStep()
        }
    }

    private fun handleExistLocate() {
        site?.let {
            val locate = Locate(
                it.id,
                it.serverId,
                getLocationGroup(it.locationGroup?.name ?: getString(R.string.none)),
                it.name,
                if (userLocation?.latitude != 0.0) userLocation?.latitude
                    ?: it.latitude else it.latitude,
                if (userLocation?.longitude != 0.0) userLocation?.longitude
                    ?: it.longitude else it.longitude,
                currentUserLocation?.altitude ?: it.altitude,
                it.createdAt,
                it.updatedAt,
                it.lastDeploymentId,
                it.lastDeploymentServerId,
                it.lastGuardianDeploymentId,
                it.lastGuardianDeploymentServerId,
                it.syncState
            )
            deploymentProtocol?.setDeployLocation(locate, true)
            deploymentProtocol?.nextStep()
        }
    }

    private fun getLocationGroup(group: String): LocationGroup {
        val locationGroup = deploymentProtocol?.getLocationGroup(group) ?: LocationGroups()
        return LocationGroup(locationGroup.name, locationGroup.color, locationGroup.serverId)
    }

    private fun getLastLocation() {
        if (latitude != 0.0 && longitude != 0.0) {
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = latitude
            loc.longitude = longitude
            userLocation = loc
        } else if (isCreateNew) {
            userLocation = currentUserLocation
                ?: deploymentProtocol?.getCurrentLocation() // get new current location
        } else {
            site?.let {
                val loc = Location(LocationManager.GPS_PROVIDER)
                loc.latitude = it.latitude
                loc.longitude = it.longitude
                userLocation = loc
            }
        }
    }

    fun updateView() {
        if (!isCreateNew) site = siteDb.getLocateById(siteId)
        if (latitude != 0.0 && longitude != 0.0) {
            val alt = if (isCreateNew) currentUserLocation?.altitude else site?.altitude
            setLatLngLabel(LatLng(latitude, longitude), alt ?: 0.0)
        } else if (isCreateNew) {
            currentUserLocation?.let { setLatLngLabel(it.toLatLng(), it.altitude) }
        } else {
            site?.let { setLatLngLabel(it.toLatLng(), it.altitude) }
            locationGroupValueTextView.text = site?.locationGroup?.name ?: getString(R.string.none)
        }
        siteValueTextView.text = siteName
        changeProjectTextView.visibility = if (isCreateNew) View.VISIBLE else View.GONE
    }

    private fun setLatLngLabel(location: LatLng, altitude: Double) {
        context?.let {
            val latLng =
                "${location.latitude.latitudeCoordinates(it)}, ${location.longitude.longitudeCoordinates(
                    it
                )}"
            coordinatesValueTextView.text = latLng
            altitudeValue.text = altitude.setFormatLabel()
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.uiSettings.setAllGesturesEnabled(false)
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false
        mapboxMap.setStyle(Style.OUTDOORS) {
            setupSymbolManager(it)
            setPinOnMap()
            enableLocationComponent()
        }
    }

    private fun setPinOnMap() {
        val curLoc = context?.getLastLocation()?.toLatLng() ?: LatLng()
        if (latitude != 0.0 && longitude != 0.0) {
            val latLng = LatLng(latitude, longitude)
            moveCamera(curLoc, latLng, DEFAULT_ZOOM)
            createSiteSymbol(latLng)
            setCheckboxForResumeDeployment(curLoc, latLng)
        } else if (!isCreateNew) {
            site = siteDb.getLocateById(siteId)
            site?.let { locate ->
                val latLng = locate.getLatLng()
                moveCamera(curLoc, latLng, DEFAULT_ZOOM)
                setCheckboxForResumeDeployment(
                    curLoc,
                    latLng
                )
                createSiteSymbol(latLng)
            }
        } else {
            createSiteSymbol(curLoc)
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

    private fun moveCamera(latLng: LatLng, zoom: Double) {
        mapboxMap?.moveCamera(MapboxCameraUtils.calculateLatLngForZoom(latLng, null, zoom))
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

        val selectedProject =
            preferences?.getString(Preferences.SELECTED_PROJECT, getString(R.string.none))
                ?: getString(R.string.none)
        val selectedGroup = preferences?.getString(Preferences.GROUP)

        group = selectedGroup ?: selectedProject
        locationGroupValueTextView.text = group
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
        private const val ARG_LATITUDE = "ARG_LATITUDE"
        private const val ARG_LONGITUDE = "ARG_LONGITUDE"

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

        fun newInstance(lat: Double, lng: Double, siteId: Int, name: String) =
            DetailDeploymentSiteFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LATITUDE, lat)
                    putDouble(ARG_LONGITUDE, lng)
                    putInt(ARG_SITE_ID, siteId)
                    putString(ARG_SITE_NAME, name)
                    putBoolean(ARG_IS_CREATE_NEW, siteId == -1)
                }
            }
    }
}
