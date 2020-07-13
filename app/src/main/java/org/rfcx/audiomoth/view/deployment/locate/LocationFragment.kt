package org.rfcx.audiomoth.view.deployment.locate


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.text.Editable
import android.text.TextWatcher
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
import androidx.core.content.res.ResourcesCompat
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
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_location.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.util.LocationPermissions
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.latitudeCoordinates
import org.rfcx.audiomoth.util.longitudeCoordinates
import org.rfcx.audiomoth.view.deployment.BaseDeploymentProtocal

class LocationFragment : Fragment(), OnMapReadyCallback {
    private val locateDb by lazy {
        LocateDb(Realm.getInstance(RealmHelper.migrationConfig()))
    }

    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView
    private lateinit var symbolManager: SymbolManager
    private var isSelectedNewLocation = false
    private var lastLocation: Location? = null
    private var locateItems = ArrayList<Locate>()
    private var locateNames = ArrayList<String>()
    private var locateItem: Locate? = null
    private var locateAdapter: ArrayAdapter<String>? = null
    private var currentUserLocation: Location? = null
    private var locationEngine: LocationEngine? = null

    private var deploymentProtocol: BaseDeploymentProtocal? = null
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
        deploymentProtocol = context as BaseDeploymentProtocal
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
        latitudeEditText.setOnEditorActionListener(editorActionListener)
        longitudeEditText.setOnEditorActionListener(editorActionListener)
        locationNameEditText.setOnEditorActionListener(editorActionListener)
    }

    private fun verifyInput() {
        if (locationNameEditText.text.toString().isNotEmpty()
            && latitudeEditText.text.toString().isNotEmpty()
            && longitudeEditText.text.toString().isNotEmpty()
        ) {
            handleNewLocate(
                latitudeEditText.text.toString().toDouble(),
                longitudeEditText.text.toString().toDouble()
            )
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

    private fun handleNewLocate(lat: Double, lng: Double) {
        val name = locationNameEditText.text.toString()
        val locate = Locate(name = name, latitude = lat, longitude = lng)
        deploymentProtocol?.setDeployLocation(locate)
        deploymentProtocol?.nextStep()
    }

    private fun setupLocationOptions(mapboxMap: MapboxMap) {
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.newLocationRadioButton -> {
                    mapboxMap.uiSettings.setAllGesturesEnabled(true)
                    onPressedNewLocation()
                }

                R.id.existingRadioButton -> {
                    mapboxMap.uiSettings.setAllGesturesEnabled(false)
                    onPressedExisting()
                }
            }
        }
    }

    private fun onPressedExisting() {
        locateItem?.let {
            setPinOnMap(it.getLatLng())
            context?.let { context ->
                setInputView(
                    locateItem?.latitude.latitudeCoordinates(context),
                    locateItem?.longitude.longitudeCoordinates(context),
                    false
                )
            }
        }
        locationNameTextInput.visibility = View.GONE
        locationNameSpinner.visibility = View.VISIBLE
    }

    private fun onPressedNewLocation() {
        isSelectedNewLocation = true
        lastLocation = currentUserLocation // get new current location
        if (lastLocation != null) {
            setInputView(
                String.format(FORMAT_DISPLAY_LOCATION, lastLocation!!.latitude),
                String.format(FORMAT_DISPLAY_LOCATION, lastLocation!!.longitude), true
            )
            setPinOnMap(LatLng(lastLocation!!.latitude, lastLocation!!.longitude))
        } else {
            // not found current location
            symbolManager.deleteAll()
            setInputView("", "", true)
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

        newLocationRadioButton.isChecked = locateItems.isEmpty()
        existingRadioButton.isEnabled = locateItems.isNotEmpty()
        existingRadioButton.isChecked = locateItems.isNotEmpty()

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
                setPinOnMap(locateItem!!.getLatLng())
                context?.let {
                    setInputView(
                        locateItem?.latitude.latitudeCoordinates(it),
                        locateItem?.longitude.longitudeCoordinates(it),
                        false
                    )
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
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false

        mapboxMap.setStyle(Style.OUTDOORS) {
            symbolManager = SymbolManager(mapView, mapboxMap, it)
            symbolManager.iconAllowOverlap = true
            symbolManager.iconIgnorePlacement = true

            lastLocation?.let { lastLocation ->
                setPinOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                setInputView(
                    String.format(FORMAT_DISPLAY_LOCATION, lastLocation.latitude),
                    String.format(FORMAT_DISPLAY_LOCATION, lastLocation.longitude), true
                )
            }
            setupLocationOptions(mapboxMap)
            enableLocationComponent()
            retrieveDeployLocations()
        }

        mapboxMap.addOnCameraMoveListener {
            val currentCameraPosition = mapboxMap.cameraPosition
            setInputView(
                String.format(FORMAT_DISPLAY_LOCATION, currentCameraPosition.target.latitude),
                String.format(FORMAT_DISPLAY_LOCATION, currentCameraPosition.target.longitude), true
            )
        }
    }

    private fun setPinOnMap(latLng: LatLng) {
        symbolManager.deleteAll()

        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map, null)
        val mBitmap = BitmapUtils.getBitmapFromDrawable(drawable)
        if (mBitmap != null) {
            mapboxMap?.style?.addImage(PIN_MAP, mBitmap)
        }

        symbolManager.create(
            SymbolOptions()
                .withLatLng(latLng)
                .withIconImage(PIN_MAP)
                .withIconSize(1.0f)
        )

        mapboxMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng,
                15.0
            )
        )
    }

    private fun setupInputLocation() {
        latitudeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null && p0.isNotBlank()) {
                    if (newLocationRadioButton.isChecked) {
                        if (p0[0] != '.' && p0.last() != '.' && !(p0[0] == '-' && p0.length == 1) && p0.last() != '-') {
                            convertInputLatitude(p0.toString())
                        }
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        longitudeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null && p0.isNotBlank()) {
                    if (newLocationRadioButton.isChecked) {
                        if (p0[0] != '.' && p0.last() != '.' && !(p0[0] == '-' && p0.length == 1) && p0.last() != '-') {
                            convertInputLongitude(p0.toString())
                        }
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    fun convertInputLatitude(latitude: String) {
        val longitude = longitudeEditText.text?.trim()
        if (longitude == null || longitude.isBlank() || longitude.last() == '-' || longitude.last() == '.') return

        val lat = latitude.toDouble()
        val long = longitude.toString()

        if (long.last() != 'E' && long.last() != 'W') {
            if (lat < 90 && lat > -90) {
                setPinOnMap(
                    LatLng(lat, longitudeEditText.text.toString().toDouble())
                )
            } else {
                Toast.makeText(
                    context,
                    getString(R.string.latitude_must_between),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun convertInputLongitude(longitude: String) {
        val latitude = latitudeEditText.text?.trim()
        if (latitude == null || latitude.isBlank() || latitude.last() == '-' || latitude.last() == '.') return

        val long = longitude.toDouble()

        if (long < 180 && long > -180) {
            setPinOnMap(
                LatLng(latitudeEditText.text.toString().toDouble(), long)
            )
        } else {
            Toast.makeText(
                context,
                getString(R.string.longitude_must_between),
                Toast.LENGTH_SHORT
            ).show()
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

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        if (checkPermissions()) {
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

    private fun setInputView(latitudeText: String, longitudeText: String, enabled: Boolean) {
        latitudeEditText.setText(latitudeText)
        latitudeEditText.isEnabled = enabled
        longitudeEditText.setText(longitudeText)
        longitudeEditText.isEnabled = enabled
        if (newLocationRadioButton.isChecked) {
            setupInputLocation()
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
        const val PIN_MAP = "pin-map"

        private const val FORMAT_DISPLAY_LOCATION = "%.6f"
        private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

        fun newInstance(): LocationFragment {
            return LocationFragment()
        }
    }
}
