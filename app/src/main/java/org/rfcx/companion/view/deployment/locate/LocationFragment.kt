package org.rfcx.companion.view.deployment.locate

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
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
import com.mapbox.pluginscalebar.ScaleBarOptions
import com.mapbox.pluginscalebar.ScaleBarPlugin
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_location.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.LocationGroup
import org.rfcx.companion.entity.LocationGroups
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol
import org.rfcx.companion.view.map.MapboxCameraUtils
import org.rfcx.companion.view.profile.locationgroup.LocationGroupActivity

class LocationFragment : Fragment(), OnMapReadyCallback {
    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val locationGroupDb = LocationGroupDb(realm)
    private val locateDb by lazy { LocateDb(realm) }

    private var mapboxMap: MapboxMap? = null
    private var symbolManager: SymbolManager? = null
    private lateinit var mapView: MapView
    private var isFirstTime = true
    private var lastLocation: Location? = null
    private var locateItems = ArrayList<Locate>()
    private var distanceLocate: ArrayList<SiteItem>? = null
    private var locateNames = ArrayList<String>()
    private var locateItem: Locate? = null
    private var locateAdapter: ArrayAdapter<String>? = null
    private var currentUserLocation: Location? = null
    private var locationEngine: LocationEngine? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    private var altitudeFromLocation: Double = 0.0
    private var nameLocation: String? = null
    private var group: String? = null
    private var isUseCurrentLocation = false

    private val analytics by lazy { context?.let { Analytics(it) } }

    private val preferences by lazy { context?.let { Preferences.getInstance(it) } }

    private var deploymentProtocol: BaseDeploymentProtocol? = null
    private val locationPermissions by lazy { activity?.let { LocationPermissions(it) } }
    private val mapboxLocationChangeCallback =
        object : LocationEngineCallback<LocationEngineResult> {
            /**
             * The LocationEngineCallback interface's method which fires when the device's location has changed.
             *
             * @param result the LocationEngineResult object which has the last known location within it.
             */
            override fun onSuccess(result: LocationEngineResult?) {
                if (activity != null) {
                    val location = result?.lastLocation

                    location ?: return
                    deploymentProtocol?.setCurrentLocation(location)

                    mapboxMap?.let {
                        this@LocationFragment.currentUserLocation = location
                        it.locationComponent.forceLocationUpdate(location)
                        altitudeFromLocation = location.altitude
                        setCurrentUserLocation()

                        altitudeValue.text = location.altitude.setFormatLabel()

                        if (isFirstTime && lastLocation == null &&
                            latitude == 0.0 && longitude == 0.0
                        ) {
                            // force update input view
                            isFirstTime = false
                            this@LocationFragment.lastLocation =
                                this@LocationFragment.currentUserLocation

                            retrieveDeployLocations()
                            setupLocationSpinner()
                            updateLocationAdapter()
                        }
                        if (!isUseCurrentLocation){
                            if (locationNameSpinner.selectedItemPosition != 0) {
                                setCheckbox()
                            }
                        }
                    }
                }
            }

            override fun onFailure(exception: Exception) {
                Log.e(TAG, exception.localizedMessage ?: "empty localizedMessage")
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationPermissions?.handleActivityResult(requestCode, resultCode)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as BaseDeploymentProtocol
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { Mapbox.getInstance(it, getString(R.string.mapbox_token)) }
        initIntent()
    }

    private fun initIntent() {
        arguments?.let {
            val currentLocation = deploymentProtocol?.getCurrentLocation()
            currentLocation?.let { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
                altitude = loc.altitude
            }
            if (it.getBoolean(ARG_FROM_PICKER)) {
                latitude = it.getDouble(ARG_LATITUDE)
                longitude = it.getDouble(ARG_LONGITUDE)
                altitude = it.getDouble(ARG_ALTITUDE)
            }
            nameLocation = it.getString(ARG_LOCATION_NAME)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }

        setViewFromDeploymentState()

        mapView = view.findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        view.viewTreeObserver.addOnGlobalLayoutListener { setOnFocusEditText() }

        setHideKeyboard()

        if (nameLocation != "" && nameLocation != null && this.nameLocation != getString(R.string.create_new_site)) {
            locationNameEditText.setText(nameLocation)
        }

        siteView.setOnClickListener {
            startSelectingExistedSite()
        }

        chooseTextView.setOnClickListener {
            startSelectingExistedSite()
        }

        currentLocateView.setOnClickListener {
            var locate = Locate()
            locateItem?.let {
                locate = Locate(
                    it.id,
                    it.serverId,
                    getLocationGroup(),
                    it.name,
                    currentUserLocation?.latitude ?: it.latitude,
                    currentUserLocation?.longitude ?: it.longitude,
                    currentUserLocation?.altitude ?: it.longitude,
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
            this.isUseCurrentLocation = true
            setWithinText()
            locateItem = locate
        }

        finishButton.setOnClickListener {
            analytics?.trackSaveLocationEvent(Screen.LOCATION.id)
            this.altitude = altitudeFromLocation
            if (locationNameSpinner.selectedItemPosition == 0) {
                getLastLocation()
                verifyInput()
            } else {
                handleExistLocate()
            }

        }

        changeTextView.setOnClickListener {
            val name = locationNameEditText.text.toString()
            startMapPicker(name, altitudeFromLocation)
            analytics?.trackChangeLocationEvent(Screen.LOCATION.id)
        }

        changeGroupTextView.setOnClickListener {
            val group = locationGroupValueTextView.text.toString()
            val setLocationGroup = if (group == getString(R.string.none)) null else group
            context?.let { it1 ->
                LocationGroupActivity.startActivity(
                    it1,
                    setLocationGroup,
                    Screen.LOCATION.id,
                    LOCATION_REQUEST_CODE
                )
                analytics?.trackChangeLocationGroupEvent(Screen.LOCATION.id)
            }
        }

        viewOfMapBox.setOnClickListener {
            if (locationNameSpinner.selectedItemPosition == 0) {
                val name = locationNameEditText.text.toString()
                startMapPicker(name, altitudeFromLocation)
                analytics?.trackChangeLocationEvent(Screen.LOCATION.id)
            }
        }

    }

    private fun startMapPicker(name: String, altitude: Double) {
        deploymentProtocol?.let {
            it.startMapPicker(latitude, longitude, altitude, name)
            it.hideToolbar()
        }
    }

    private fun startSelectingExistedSite() {
        val locate = if (lastLocation == null) currentUserLocation else lastLocation
        locate?.let { lastLocate ->
            deploymentProtocol?.let {
                it.startSelectingExistedSite(lastLocate.latitude, lastLocate.longitude)
            }
        }
    }

    private fun setViewFromDeploymentState() {
        val fromUnfinishedDeployment =
            deploymentProtocol?.isOpenedFromUnfinishedDeployment() ?: false
        changeGroupTextView.isEnabled = !fromUnfinishedDeployment
        if (fromUnfinishedDeployment) {
            changeGroupTextView.visibility = View.INVISIBLE
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.uiSettings.setAllGesturesEnabled(false)
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false

        mapboxMap.setStyle(Style.OUTDOORS) {
            lastLocation?.let { lastLocation ->
                val latLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                moveCamera(latLng, null, DEFAULT_ZOOM)
            }
            setupSymbolManager(it)
            retrieveDeployLocations()
            setupLocationSpinner()
            updateLocationAdapter()
            enableLocationComponent()
            setupScale()
        }
    }

    private fun setupSymbolManager(style: Style) {
        this.mapboxMap?.let { mapboxMap ->
            symbolManager = SymbolManager(this.mapView, mapboxMap, style)
            symbolManager?.iconAllowOverlap = true

            style.addImage(PROPERTY_MARKER_IMAGE, ResourcesCompat.getDrawable(this.resources, R.drawable.ic_pin_map, null)!!)
        }
    }

    private fun createSiteSymbol(latLng: LatLng) {
        symbolManager?.deleteAll()
        symbolManager?.create(SymbolOptions()
            .withLatLng(latLng)
            .withIconImage(PROPERTY_MARKER_IMAGE)
            .withIconSize(0.75f))
    }

    private fun verifyInput() {
        val name = locationNameEditText.text.toString()
        val locationValue = locationValueTextView.text.toString()
        if (name.isNotEmpty() && locationValue.isNotEmpty() && lastLocation != null) {
            lastLocation?.let {

                val locate = Locate(
                    name = name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    altitude = altitude,
                    locationGroup = getLocationGroup()
                )
                deploymentProtocol?.setDeployLocation(locate, false)
                deploymentProtocol?.nextStep()
            }
        } else {
            Toast.makeText(
                context,
                getString(R.string.please_fill_information),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleExistLocate() {
        locateItem?.let {
            val locate = Locate(
                it.id,
                it.serverId,
                getLocationGroup(),
                it.name,
                it.latitude,
                it.longitude,
                currentUserLocation?.altitude ?: it.longitude,
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

    private fun getLocationGroup(): LocationGroup {
        val group = this.group ?: getString(R.string.none)
        val locationGroup = deploymentProtocol?.getLocationGroup(group) ?: LocationGroups()
        return LocationGroup(locationGroup.name, locationGroup.color, locationGroup.serverId)
    }

    private fun setLatLogLabel(location: LatLng) {
        context?.let {
            val latLng =
                "${location.latitude.latitudeCoordinates(it)}, ${location.longitude.longitudeCoordinates(
                    it
                )}"
            locationValueTextView.text = latLng
        }
    }

    private fun onPressedExisting() {
        getLastLocation()
        enableExistingLocation(true)

        locateItem?.let {
            createSiteSymbol(it.getLatLng())
            moveCamera(LatLng(latitude, longitude), it.getLatLng(), DEFAULT_ZOOM)
            if (locationGroupDb.isExisted(it.locationGroup?.name)) {
                group = it.locationGroup?.name
                locationGroupValueTextView.text = it.locationGroup?.name
            } else {
                group = getString(R.string.none)
                locationGroupValueTextView.text = getString(R.string.none)
            }
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

    fun setCheckbox() {
        locateItem?.let {
            val locate = if (lastLocation == null) currentUserLocation else lastLocation
            val nearLocations = findNearLocations(locate, locateItems)
            distanceLocate = nearLocations?.let { location ->
                ArrayList(location.map {
                    SiteItem(
                        it.first,
                        it.second
                    )
                })
            }

            val siteItem = distanceLocate?.filter{ site ->
                site.locate.id == it.id
            }
            if (siteItem != null && siteItem.isNotEmpty()) {
                if (siteItem[0].distance <= 20) {
                    setWithinText()
                } else {
                    setNotWithinText(siteItem[0].distance.setFormatLabel())
                }
            }
        }
    }

    private fun setCheckboxForResumeDeployment(curLoc: LatLng, target: LatLng) {
        val distance = curLoc.distanceTo(target)
        if (distance <= 20) {
            setWithinText()
        } else {
            setNotWithinText(distance.setFormatLabel())
        }
    }

    private fun getLastLocation() {
        if (latitude != 0.0 && longitude != 0.0) {
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = latitude
            loc.longitude = longitude
            lastLocation = loc
        } else {
            lastLocation = currentUserLocation ?: deploymentProtocol?.getCurrentLocation() // get new current location
            this.latitude = lastLocation?.latitude ?: 0.0
            this.longitude = lastLocation?.longitude ?: 0.0
        }
    }

    private fun onPressedNewLocation() {
        enableExistingLocation(false)
        siteValueTextView.text = getString(R.string.create_new_site)
        altitudeValue.text = altitudeFromLocation.setFormatLabel()
        getLastLocation()

        val selectedProject = preferences?.getString(Preferences.SELECTED_PROJECT, getString(R.string.none)) ?: getString(R.string.none)
        group = selectedProject
        locationGroupValueTextView.text = selectedProject

        if (lastLocation != null) {
            lastLocation?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                createSiteSymbol(latLng)
                distanceSite(it.latitude, it.longitude)
                val currentLocation = deploymentProtocol?.getCurrentLocation()
                if (currentLocation != null) {
                    moveCamera(LatLng(currentLocation.latitude, currentLocation.longitude), latLng, DEFAULT_ZOOM)
                } else {
                    moveCamera(latLng, null, DEFAULT_ZOOM)
                }
            }
        } else {
            // not found current location
            setLatLogLabel(LatLng(0.0, 0.0))
            moveCamera(LatLng(0.0, 0.0), DEFAULT_ZOOM)
            distanceSite(0.0,0.0)
        }
    }

    private fun distanceSite(lat: Double, lng: Double) {
        val loc = Location(LocationManager.GPS_PROVIDER)
        loc.latitude = lat
        loc.longitude = lng

        val distance = loc.distanceTo(this.currentUserLocation)
        if (distance <= 20) {
            setWithinText()
        } else {
            setNotWithinText(distance.setFormatLabel())
        }
    }

    private fun setCurrentUserLocation() {
        currentUserLocation?.let { lastLocation ->
            val latLng = LatLng(lastLocation.latitude, lastLocation.longitude)
            setLatLogLabel(latLng)
        }
    }

    private fun updateLocationAdapter() {
        // already have deployment location?
        val deploymentLocation = deploymentProtocol?.getDeploymentLocation()
        if (deploymentLocation != null && locateAdapter != null && latitude == 0.0 && longitude == 0.0) {
            val spinnerPosition = locateAdapter!!.getPosition(deploymentLocation.name)
            locationNameSpinner.setSelection(spinnerPosition)
            siteValueTextView.text = deploymentLocation.name
            val position = locateNames.indexOf(deploymentLocation.name)
            if (position >= 0) {
                var locate = Locate()
                locateItem = locateItems[position]
                locateItem?.let {
                    locate = Locate(
                        it.id,
                        it.serverId,
                        getLocationGroup(),
                        it.name,
                        deploymentLocation.latitude,
                        deploymentLocation.longitude,
                        deploymentLocation.altitude,
                        it.createdAt,
                        it.updatedAt,
                        it.lastDeploymentId,
                        it.lastDeploymentServerId,
                        it.lastGuardianDeploymentId,
                        it.lastGuardianDeploymentServerId,
                        it.syncState
                    )
                }
                locateItem = locate
                val currentSite = locateItems.findLast {
                    it.serverId == deploymentLocation.coreId
                }
                if (currentSite != null) {
                    locateItems.remove(currentSite)
                    locateItems.add(locate)
                }
            }

            //assign to make setCheckbox work
            val currentLocation = deploymentProtocol!!.getCurrentLocation()
            val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
            val siteLatLng = LatLng(locateItem?.latitude ?: deploymentLocation.latitude, locateItem?.longitude ?: deploymentLocation.longitude)
            getLastLocation()
            createSiteSymbol(siteLatLng)
            setCheckboxForResumeDeployment(currentLatLng, siteLatLng)

            enableExistingLocation(true)
            moveCamera(currentLatLng, siteLatLng, DEFAULT_ZOOM)
            if (locationGroupDb.isExisted(deploymentLocation.project?.name)) {
                group = deploymentLocation.project?.name
                locationGroupValueTextView.text = deploymentLocation.project?.name
            } else {
                group = getString(R.string.none)
                locationGroupValueTextView.text = getString(R.string.none)
            }
        } else {
            this.currentUserLocation = deploymentProtocol?.getCurrentLocation()
            val locate = if (lastLocation == null) currentUserLocation else lastLocation
            val nearLocations = findNearLocations(locate, locateItems)
            val nearItems =
                nearLocations?.filter { it.second < 10000 } ?: listOf() // 10000m == 10km
            if (latitude == 0.0 && longitude == 0.0 && this.nameLocation != getString(R.string.create_new_site)) {
                if (nearItems.isNotEmpty()) {
                    val nearItem = nearItems.minBy { it.second }
                    val position = locateItems.indexOf(nearItem?.first)
                    locationNameSpinner.setSelection(position)
                    locateItem = locateItems[position]
                    siteValueTextView.text = locateItems[position].name
                    onPressedExisting()
                } else {
                    onPressedNewLocation()
                }
            } else {
                if (locationNameSpinner.selectedItemPosition == 0) {
                    onPressedNewLocation()
                } else {
                    onPressedExisting()
                }
            }
        }
    }

    private fun setupLocationSpinner() {
        this.locateAdapter?.clear()
        this.locateAdapter?.notifyDataSetChanged()

        locateItems.mapTo(locateNames, { it.name })
        this.locateAdapter = context?.let {
            ArrayAdapter(
                it,
                R.layout.support_simple_spinner_dropdown_item,
                locateNames
            )
        }
        locationNameSpinner.adapter = locateAdapter
        if (nameLocation != "" && nameLocation != null) {
            val name = nameLocation
            val position = locateNames.indexOf(name)
            if (position >= 0) {
                locationNameSpinner.setSelection(position)
                siteValueTextView.text = locateItems[position].name
                locateItem = locateItems[position]
            }
        }
    }

    private fun retrieveDeployLocations() {
        locateItems.clear()
        val locations = locateDb.getLocations()
        val nearLocations =
            findNearLocations(lastLocation, ArrayList(locations))?.sortedBy { it.second }
        val locationsItems = nearLocations?.map { it.first }
        val createNew = listOf(Locate(id = -1, name = getString(R.string.create_new_site)))
        if (locationsItems != null) {
            locateItems.addAll(createNew + locationsItems)
        } else {
            locateItems.addAll(createNew + locations)
        }
    }

    /**
     * Return [List<Locate, Distance>]]
     * */
    private fun findNearLocations(
        lastLocation: Location?,
        locateItems: ArrayList<Locate>
    ): List<Pair<Locate, Float>>? {
        lastLocation ?: return null

        if (locateItems.isNotEmpty()) {
            val itemsWithDistance = arrayListOf<Pair<Locate, Float>>()
            // Find locate distances
            locateItems.mapTo(itemsWithDistance, {
                val loc = Location(LocationManager.GPS_PROVIDER)
                loc.latitude = it.latitude
                loc.longitude = it.longitude
                val distance = loc.distanceTo(this.currentUserLocation) // return in meters
                Pair(it, distance)
            })
            return itemsWithDistance
        }
        return null
    }

    private fun moveCamera(latLng: LatLng, zoom: Double) {
        mapboxMap?.moveCamera(MapboxCameraUtils.calculateLatLngForZoom(latLng, null, zoom))
    }

    private fun moveCamera(userPosition: LatLng, nearestSite: LatLng?, zoom: Double) {
        mapboxMap?.moveCamera(MapboxCameraUtils.calculateLatLngForZoom(userPosition, nearestSite, zoom))
    }

    private fun setOnFocusEditText() {
        val screenHeight: Int = view?.rootView?.height ?: 0
        val r = Rect()
        view?.getWindowVisibleDisplayFrame(r)
        val keypadHeight: Int = screenHeight - r.bottom
        if (keypadHeight > screenHeight * 0.15) {
            finishButton.visibility = View.GONE
        } else {
            if (finishButton != null) {
                finishButton.visibility = View.VISIBLE
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableLocationComponent()
            }
        }
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
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        } else {
            throw Exception("Request permissions not required before API 23 (should never happen)")
        }
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
            this.lastLocation = this.currentUserLocation

            initLocationEngine()
        } else {
            requestPermissions()
        }
    }

    private fun setupScale() {
        val scaleBarPlugin = ScaleBarPlugin(mapView, mapboxMap!!)
        scaleBarPlugin.create(ScaleBarOptions(requireContext()))
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        locationEngine = context?.let { LocationEngineProvider.getBestLocationEngine(it) }
        val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()

        locationEngine?.requestLocationUpdates(
            request,
            mapboxLocationChangeCallback,
            getMainLooper()
        )

        locationEngine?.getLastLocation(mapboxLocationChangeCallback)
    }

    private fun enableExistingLocation(enable: Boolean) {
        locationNameTextInput.visibility = if (enable) View.GONE else View.VISIBLE
        changeTextView.visibility = if (enable) View.GONE else View.VISIBLE
        changeGroupTextView.visibility = if (enable) View.GONE else View.VISIBLE
        currentLocateGroupView.visibility = if (enable) View.VISIBLE else View.GONE
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        analytics?.trackScreen(Screen.LOCATION)

        val selectedProject = preferences?.getString(Preferences.SELECTED_PROJECT, getString(R.string.none)) ?: getString(R.string.none)
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
        locationEngine?.removeLocationUpdates(mapboxLocationChangeCallback)
        mapView.onDestroy()
    }

    private fun setHideKeyboard() {
        val editorActionListener =
            TextView.OnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    v.clearFocus()
                    v.hideKeyboard()
                }
                false
            }
        locationNameEditText.setOnEditorActionListener(editorActionListener)
    }

    private fun View.hideKeyboard() = this.let {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    companion object {
        const val TAG = "LocationFragment"
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        const val LOCATION_REQUEST_CODE = 1003
        const val DEFAULT_ZOOM = 15.0
        const val ARG_LATITUDE = "ARG_LATITUDE"
        const val ARG_LONGITUDE = "ARG_LONGITUDE"
        const val ARG_ALTITUDE = "ARG_ALTITUDE"
        const val ARG_LOCATION_NAME = "ARG_LOCATION_NAME"
        const val ARG_FROM_PICKER = "ARG_FROM_PICKER"

        const val PROPERTY_MARKER_IMAGE = "marker2.image"

        const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

        fun newInstance(): LocationFragment {
            return LocationFragment()
        }

        fun newInstance(latitude: Double, longitude: Double, altitude: Double, name: String, fromPicker: Boolean) =
            LocationFragment()
                .apply {
                    arguments = Bundle().apply {
                        putDouble(ARG_LATITUDE, latitude)
                        putDouble(ARG_LONGITUDE, longitude)
                        putDouble(ARG_ALTITUDE, altitude)
                        putString(ARG_LOCATION_NAME, name)
                        putBoolean(ARG_FROM_PICKER, fromPicker)
                    }
                }
    }
}
