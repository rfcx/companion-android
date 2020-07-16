package org.rfcx.audiomoth.view.deployment.locate


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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
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
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_location.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.util.LocationPermissions
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.latitudeCoordinates
import org.rfcx.audiomoth.util.longitudeCoordinates
import org.rfcx.audiomoth.view.deployment.DeploymentProtocol

class LocationFragment : Fragment(), OnMapReadyCallback {
    private val locateDb by lazy {
        LocateDb(Realm.getInstance(RealmHelper.migrationConfig()))
    }

    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView
    private var isSelectedNewLocation = false
    private var lastLocation: Location? = null
    private var locateItems = ArrayList<Locate>()
    private var locateNames = ArrayList<String>()
    private var locateItem: Locate? = null
    private var locateAdapter: ArrayAdapter<String>? = null
    private var currentUserLocation: Location? = null
    private var locationEngine: LocationEngine? = null
    private var latLng: LatLng? = null

    private var deploymentProtocol: DeploymentProtocol? = null
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
                        if (isSelectedNewLocation && lastLocation == null) {
                            // force update input view
                            onPressedNewLocation()
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
        deploymentProtocol = context as DeploymentProtocol
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { Mapbox.getInstance(it, getString(R.string.mapbox_token)) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        view.viewTreeObserver.addOnGlobalLayoutListener { setOnFocusEditText() }

        deploymentProtocol?.showStepView()
        deploymentProtocol?.hideCompleteButton()

        setHideKeyboard()

        finishButton.setOnClickListener {
            if (existingRadioButton.isChecked) {
                handleExistLocate()
            } else if (newLocationRadioButton.isChecked) {
                verifyInput()
            }
        }

        changeTextView.setOnClickListener {
            deploymentProtocol?.startMapPicker()
        }

        viewOfMapBox.setOnClickListener {
            if (newLocationRadioButton.isChecked) {
                deploymentProtocol?.startMapPicker()
            }
        }
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

    private fun verifyInput() {
        val name = locationNameEditText.text.toString()
        val locationValue = locationValueTextView.text.toString()
        if (name.isNotEmpty() && locationValue.isNotEmpty() && lastLocation != null) {
            lastLocation?.let {
                val locate = Locate(name = name, latitude = it.latitude, longitude = it.longitude)
                deploymentProtocol?.setDeployLocation(locate)
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
            deploymentProtocol?.setDeployLocation(it)
            deploymentProtocol?.nextStep()
        }
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

    private fun onPressedNewLocation() {
        var lat: Double? = null
        var lng: Double? = null
        isSelectedNewLocation = true

        arguments?.let {
            lat = it.getDouble(LATITUDE_VALUE)
            lng = it.getDouble(LONGITUDE_VALUE)
        }

        if (lat != null && lng != null) {
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = lat as Double
            loc.longitude = lng as Double
            lastLocation = loc
        } else {
            lastLocation = currentUserLocation // get new current location
        }

        if (lastLocation != null) {
            lastLocation?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                setLatLogLabel(latLng)
                moveCamera(latLng, DEFAULT_ZOOM)
            }
        } else {
            // not found current location
            setLatLogLabel(LatLng(0.0, 0.0))
        }
        locationNameTextInput.visibility = View.VISIBLE
        locationNameSpinner.visibility = View.GONE
    }

    private fun setLocationAdapter() {
        isSelectedNewLocation = false
        locateItems.mapTo(locateNames, { it.name })
        locateAdapter = context?.let {
            ArrayAdapter(
                it,
                R.layout.support_simple_spinner_dropdown_item,
                locateNames
            )
        }
        locationNameSpinner.adapter = locateAdapter
        setLocationSpinner()
        setRecommendLocation()

        val value = arguments?.getDouble(LATITUDE_VALUE)
        if (value != null) {
            newLocationRadioButton.isChecked = true
            existingRadioButton.isChecked = false
        } else {
            newLocationRadioButton.isChecked = locateItems.isEmpty()
            existingRadioButton.isEnabled = locateItems.isNotEmpty()
            existingRadioButton.isChecked = locateItems.isNotEmpty()
        }

        val deploymentLocation = deploymentProtocol?.getDeploymentLocation()
        if (deploymentLocation != null && locateAdapter != null) {
            val spinnerPosition = locateAdapter!!.getPosition(deploymentLocation.name)
            locationNameSpinner.setSelection(spinnerPosition)
        }
    }

    private fun setLocationSpinner() {
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
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun retrieveDeployLocations() {
        locateItems.clear()
        val locations = locateDb.getLocations()
        locateItems.addAll(locations)
        setLocationAdapter()
    }

    private fun setRecommendLocation() {
        lastLocation ?: return

        if (locateItems.isNotEmpty()) {
            val itemsWithDistance = arrayListOf<Pair<Locate, Float>>()
            // Find locate distances
            locateItems.mapTo(itemsWithDistance, {
                val loc = Location(LocationManager.GPS_PROVIDER)
                loc.latitude = it.latitude
                loc.longitude = it.longitude
                val distance = loc.distanceTo(lastLocation)
                Pair(it, distance)
            })
            val nearItem = itemsWithDistance.minBy { it.second }
            val position = locateItems.indexOf(nearItem?.first)
            locationNameSpinner.setSelection(position)
            locateItem = locateItems[position]
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
            setupLocationOptions()
            enableLocationComponent()
            retrieveDeployLocations()
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Double) {
        mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
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
            // Enable to make component visible
            locationComponent?.isLocationComponentEnabled = false
            // Set the component's render mode
            locationComponent?.renderMode = RenderMode.COMPASS

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

    private fun View.hideKeyboard() = this.let {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    companion object {
        const val TAG = "LocationFragment"
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        const val DEFAULT_ZOOM = 15.0
        const val LATITUDE_VALUE = "LATITUDE_VALUE"
        const val LONGITUDE_VALUE = "LONGITUDE_VALUE"

        private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

        fun newInstance(): LocationFragment {
            return LocationFragment()
        }

        fun newInstance(latitude: Double, longitude: Double) = LocationFragment()
            .apply {
                arguments = Bundle().apply {
                    putDouble(LATITUDE_VALUE, latitude)
                    putDouble(LONGITUDE_VALUE, longitude)
                }
            }
    }
}
