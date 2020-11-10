package org.rfcx.companion.view.deployment.locate

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.mapbox.android.core.location.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
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
import org.rfcx.companion.view.profile.locationgroup.LocationGroupActivity

class LocationFragment : Fragment(), OnMapReadyCallback {
    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val locationGroupDb = LocationGroupDb(realm)
    private val locateDb by lazy { LocateDb(realm) }

    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView
    private var isFirstTime = true
    private var lastLocation: Location? = null
    private var locateItems = ArrayList<Locate>()
    private var locateNames = ArrayList<String>()
    private var locateItem: Locate? = null
    private var locateAdapter: ArrayAdapter<String>? = null
    private var currentUserLocation: Location? = null
    private var locationEngine: LocationEngine? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var nameLocation: String? = null
    private var group: String? = null

    private val analytics by lazy { context?.let { Analytics(it) } }

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

                    mapboxMap?.let {
                        this@LocationFragment.currentUserLocation = location
                        it.locationComponent.forceLocationUpdate(location)
                        if (isFirstTime && lastLocation == null &&
                            latitude == 0.0 && longitude == 0.0
                        ) {
                            // force update input view
                            isFirstTime = false
                            this@LocationFragment.lastLocation =
                                this@LocationFragment.currentUserLocation
                            updateLocationAdapter()
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
            latitude = it.getDouble(ARG_LATITUDE)
            longitude = it.getDouble(ARG_LONGITUDE)
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

        if (nameLocation != "" && nameLocation != null) {
            locationNameEditText.setText(nameLocation)
        }

        finishButton.setOnClickListener {
            if (existingRadioButton.isChecked) {
                handleExistLocate()
            } else if (newLocationRadioButton.isChecked) {
                getLastLocation()
                verifyInput()
            }
        }

        changeTextView.setOnClickListener {
            val name = locationNameEditText.text.toString()
            startMapPicker(name)
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
            }
        }

        viewOfMapBox.setOnClickListener {
            if (newLocationRadioButton.isChecked) {
                val name = locationNameEditText.text.toString()
                startMapPicker(name)
            }
        }
    }

    private fun startMapPicker(name: String) {
        deploymentProtocol?.let {
            it.startMapPicker(latitude, longitude, name)
            it.hideToolbar()
        }
    }

    private fun setViewFromDeploymentState() {
        val fromUnfinishedDeployment = deploymentProtocol?.isOpenedFromUnfinishedDeployment() ?: false
        existingRadioButton.isEnabled = !fromUnfinishedDeployment
        newLocationRadioButton.isEnabled = !fromUnfinishedDeployment
        locationNameSpinner.isEnabled = !fromUnfinishedDeployment
        changeGroupTextView.isEnabled = !fromUnfinishedDeployment
        if (fromUnfinishedDeployment) {
            existingRadioButton.isClickable = false
            existingRadioButton.alpha = 0.5f
            newLocationRadioButton.alpha = 0.5f
            changeGroupTextView.alpha = 0.5f
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
                moveCamera(latLng, DEFAULT_ZOOM)
                setLatLogLabel(latLng)
            }
            retrieveDeployLocations()
            setupLocationSpinner()
            setupLocationOptions()
            updateLocationAdapter()
            enableLocationComponent()
        }
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
                it.createdAt,
                it.deletedAt,
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

    private fun setupLocationOptions() {
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.newLocationRadioButton -> {
                    changeTextView.visibility = View.VISIBLE
                    onPressedNewLocation()
                }

                R.id.existingRadioButton -> {
                    changeTextView.visibility = View.GONE
                    onPressedExisting()
                }
            }
        }
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
        locateItem?.let {
            moveCamera(it.getLatLng(), DEFAULT_ZOOM)
            setLatLogLabel(it.getLatLng())
        }
        locationNameTextInput.visibility = View.GONE
        locationNameSpinner.visibility = View.VISIBLE
    }

    private fun getLastLocation() {
        if (latitude != 0.0 && longitude != 0.0) {
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = latitude
            loc.longitude = longitude
            lastLocation = loc
        } else {
            lastLocation = currentUserLocation // get new current location
            this.latitude = lastLocation?.latitude ?: 0.0
            this.longitude = lastLocation?.longitude ?: 0.0
        }
    }

    private fun onPressedNewLocation() {
        getLastLocation()

        if (lastLocation != null) {
            lastLocation?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                setLatLogLabel(latLng)
                moveCamera(latLng, DEFAULT_ZOOM)
            }
        } else {
            // not found current location
            setLatLogLabel(LatLng(0.0, 0.0))
            moveCamera(LatLng(0.0, 0.0), DEFAULT_ZOOM)
        }
        locationNameTextInput.visibility = View.VISIBLE
        locationNameSpinner.visibility = View.GONE
    }

    private fun updateLocationAdapter() {
        // already have deployment location?
        val deploymentLocation = deploymentProtocol?.getDeploymentLocation()
        if (deploymentLocation != null && locateAdapter != null) {
            val spinnerPosition = locateAdapter!!.getPosition(deploymentLocation.name)
            // enable exiting radio button
            enableExistingLocationButton()
            locationNameSpinner.setSelection(spinnerPosition)
        } else {
            val nearLocations = findNearLocations(lastLocation, locateItems)

            // lat & lng from selecting new location
            if (locateItems.isNotEmpty() && nearLocations != null &&
                latitude == 0.0 && longitude == 0.0
            ) {
                // enable exiting radio button
                enableExistingLocationButton()
                // set selected locate Item
                val nearItem = nearLocations.minBy { it.second }
                val position = locateItems.indexOf(nearItem?.first)
                locationNameSpinner.setSelection(position)
                locateItem = locateItems[position]

                onPressedExisting()
            } else {
                enableNewLocationButton()
                onPressedNewLocation()
                if (locateItems.isNullOrEmpty()) {
                    existingRadioButton.isEnabled = false
                }
            }
        }
    }

    private fun enableExistingLocationButton() {
        existingRadioButton.isChecked = true
        existingRadioButton.isEnabled = true
        newLocationRadioButton.isChecked = false
    }

    private fun enableNewLocationButton() {
        newLocationRadioButton.isChecked = true
        existingRadioButton.isChecked = false
    }

    private fun setupLocationSpinner() {
        locateItems.mapTo(locateNames, { it.name })
        this.locateAdapter = context?.let {
            ArrayAdapter(
                it,
                R.layout.support_simple_spinner_dropdown_item,
                locateNames
            )
        }
        locationNameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                locateItem = locateItems[position]
                locateItem?.let {
                    val latLng = it.getLatLng()
                    moveCamera(latLng, DEFAULT_ZOOM)
                    setLatLogLabel(latLng)

                    if (locationGroupDb.isExisted(it.locationGroup?.group)) {
                        group = it.locationGroup?.group
                        locationGroupValueTextView.text = it.locationGroup?.group
                        it.locationGroup?.color?.let { it1 -> setPinColorByGroup(it1) }
                    } else {
                        group = getString(R.string.none)
                        locationGroupValueTextView.text = getString(R.string.none)
                        it.locationGroup?.color?.let { setPinColorByGroup("#2AA841") }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        locationNameSpinner.adapter = locateAdapter
    }

    private fun retrieveDeployLocations() {
        locateItems.clear()
        val locations = locateDb.getLocations()
        val showLocations = locations.filter { it.isCompleted() }
        locateItems.addAll(showLocations)
    }

    /**
     * Return [List<Locate, Distance( < 50m )>]]
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
                val distance = loc.distanceTo(this.lastLocation) // return in meters
                Pair(it, distance)
            })
            val nearItems = itemsWithDistance.filter { it.second < 50 }
            return if (nearItems.isEmpty()) null else nearItems
        }
        return null
    }

    private fun moveCamera(latLng: LatLng, zoom: Double) {
        mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    private fun changePinColorByGroup(group: String) {
        val locationGroup = deploymentProtocol?.getLocationGroup(group)
        val color = locationGroup?.color
        if (color != null) {
            setPinColorByGroup(color)
        }
    }

    private fun setPinColorByGroup(color: String) {
        val pinDrawable = pinDeploymentImageView.drawable
        if (color.isNotEmpty() && group != getString(R.string.none)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                pinDrawable.setColorFilter(color.toColorInt(), PorterDuff.Mode.SRC_ATOP)
            } else {
                pinDrawable.setTint(color.toColorInt())
            }
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                pinDrawable.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimary
                    ), PorterDuff.Mode.SRC_ATOP
                )
            } else {
                pinDrawable.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            }
        }
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
            context?.let {
                locationComponent?.activateLocationComponent(
                    LocationComponentActivationOptions.builder(it, loadedMapStyle!!)
                        .useDefaultLocationEngine(false)
                        .build()
                )
            }

            this.currentUserLocation = locationComponent?.lastKnownLocation
            this.lastLocation = this.currentUserLocation

            initLocationEngine()
        } else {
            requestPermissions()
        }
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

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        analytics?.trackScreen(Screen.LOCATION)

        val preferences = context?.let { Preferences.getInstance(it) }
        group = preferences?.getString(Preferences.GROUP, getString(R.string.none))
        locationGroupValueTextView.text = group
        changePinColorByGroup(group ?: "")
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
        const val ARG_LOCATION_NAME = "ARG_LOCATION_NAME"

        const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

        fun newInstance(): LocationFragment {
            return LocationFragment()
        }

        fun newInstance(latitude: Double, longitude: Double, name: String) = LocationFragment()
            .apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LATITUDE, latitude)
                    putDouble(ARG_LONGITUDE, longitude)
                    putString(ARG_LOCATION_NAME, name)
                }
            }
    }
}
