package org.rfcx.audiomoth.view.deployment.locate


import android.Manifest
import android.content.Context
import android.content.Intent
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
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
import org.rfcx.audiomoth.view.deployment.DeploymentProtocol

class LocationFragment : Fragment(), OnMapReadyCallback {
    private val locateDb by lazy {
        LocateDb(Realm.getInstance(RealmHelper.migrationConfig()))
    }

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapView: MapView
    private lateinit var symbolManager: SymbolManager
    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null
    private var locateItems = ArrayList<Locate>()
    private var locateNames = ArrayList<String>()
    private var locateItem: Locate? = null
    private var locateAdapter: ArrayAdapter<String>? = null

    private var deploymentProtocol: DeploymentProtocol? = null
    private val locationPermissions by lazy { activity?.let { LocationPermissions(it) } }

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

        deploymentProtocol?.showStepView()
        deploymentProtocol?.hideCompleteButton()

        finishButton.setOnClickListener {
            if (existingRadioButton.isChecked) {
                handleExistLocate()
            } else if (newLocationRadioButton.isChecked) {
                verifyInput()
            }
        }
    }

    private fun verifyInput() {
        if (locationNameEditText.text.toString().isNotEmpty()
            && latitudeEditText.text.toString().isNotEmpty()
            && longitudeEditText.text.toString().isNotEmpty()
        ) {
            handleNewLocate()
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

    private fun handleNewLocate() {
        val name = locationNameEditText.text.toString()
        val lat = latitudeEditText.text.toString().toDouble()
        val lng = longitudeEditText.text.toString().toDouble()
        val locate = Locate(name = name, latitude = lat, longitude = lng)
        deploymentProtocol?.setDeployLocation(locate)
        deploymentProtocol?.nextStep()
    }

    private fun setupLocationOptions() {
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.newLocationRadioButton -> {
                    lastLocation?.let { lastLocation ->
                        setupView(
                            String.format("%.6f", lastLocation.latitude),
                            String.format("%.6f", lastLocation.longitude), true
                        )
                        setPinOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                    }
                    locationNameTextInput.visibility = View.VISIBLE
                    locationNameSpinner.visibility = View.GONE
                }

                R.id.existingRadioButton -> {
                    locateItem?.let {
                        setPinOnMap(it.getLatLng())
                        setupView(it.latitude.toString(), it.longitude.toString(), false)
                    }
                    locationNameTextInput.visibility = View.GONE
                    locationNameSpinner.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setLocationAdapter() {
        context ?: return

        locateItems.mapTo(locateNames, { it.name })
        locateAdapter =
            ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, locateNames)
        locationNameSpinner.adapter = locateAdapter
        setLocationSpinner()
        setRecommendLocation()

        newLocationRadioButton.isChecked = locateItems.isEmpty()
        existingRadioButton.isEnabled = locateItems.isNotEmpty()
        existingRadioButton.isChecked = locateItems.isNotEmpty()
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
                setupView(
                    locateItem?.latitude.toString(),
                    locateItem?.longitude.toString(),
                    false
                )
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
        mapboxMap.setStyle(Style.OUTDOORS) {
            symbolManager = SymbolManager(mapView, mapboxMap, it)
            symbolManager.iconAllowOverlap = true
            symbolManager.iconIgnorePlacement = true

            lastLocation?.let { lastLocation ->
                setPinOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                setupView(
                    String.format("%.6f", lastLocation.latitude),
                    String.format("%.6f", lastLocation.longitude), true
                )
            }
            setupInputLocation()
            setupLocationOptions()

            getLastLocation()
            checkThenAcquireLocation(it)
            retrieveDeployLocations()
        }
    }

    private fun checkThenAcquireLocation(style: Style) {
        locationPermissions?.check { isAllowed: Boolean ->
            if (isAllowed) {
                enableLocationComponent(style)
            }
        }
    }

    private fun enableLocationComponent(style: Style) {
        val customLocationComponentOptions = context?.let {
            LocationComponentOptions.builder(it)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(it, R.color.colorPrimary))
                .build()
        }

        val locationComponentActivationOptions =
            context?.let {
                LocationComponentActivationOptions.builder(it, style)
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

    private fun setupInputLocation() {
        latitudeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null) {
                    if (p0.toString() != "-" && p0.isNotEmpty() && p0.toString() != "." && longitudeEditText.text.toString()
                            .isNotEmpty()
                    ) {
                        if (p0.toString().toDouble() >= -90.0 && p0.toString().toDouble() <= 90) {
                            setPinOnMap(
                                LatLng(
                                    p0.toString().toDouble(),
                                    longitudeEditText.text.toString().toDouble()
                                )
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
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        longitudeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null) {
                    if (p0.toString() != "-" && p0.isNotEmpty() && p0.toString() != "." && latitudeEditText.text.toString()
                            .isNotEmpty()
                    ) {
                        if (latitudeEditText.text.toString()
                                .toDouble() >= -90.0 && latitudeEditText.text.toString()
                                .toDouble() <= 90
                        ) {
                            setPinOnMap(
                                LatLng(
                                    latitudeEditText.text.toString().toDouble(),
                                    p0.toString().toDouble()
                                )
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
                lastLocation =
                    locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)

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
        locationPermissions?.handleRequestResult(requestCode, grantResults)

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

    private fun setupView(latitudeText: String, longitudeText: String, enabled: Boolean) {
        latitudeEditText.setText(latitudeText)
        latitudeEditText.isEnabled = enabled
        longitudeEditText.setText(longitudeText)
        longitudeEditText.isEnabled = enabled
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
        const val TAG = "LocationFragment"
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        const val PIN_MAP = "pin-map"

        fun newInstance(): LocationFragment {
            return LocationFragment()
        }
    }
}
