package org.rfcx.audiomoth.view.deployment.locate


import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_location.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.util.*
import org.rfcx.audiomoth.view.deployment.DeploymentProtocol
import org.rfcx.audiomoth.view.profile.coordinates.CoordinatesActivity

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
            setLocate()
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

    private fun setLocate() {
        when (context.getCoordinatesFormat()) {
            CoordinatesActivity.DD_FORMAT -> {
                if (latitudeEditText.text.toString().matches(FORMAT_LATITUDE_DD.toRegex())) {
                    if (longitudeEditText.text.toString().matches(FORMAT_LONGITUDE_DD.toRegex())) {
                        handleNewLocate(
                            latitudeEditText.text.toString().replaceDDToNumber(),
                            longitudeEditText.text.toString().replaceDDToNumber()
                        )
                    } else {
                        longitudeEditText.error = getString(R.string.wrong_format)
                    }
                } else {
                    latitudeEditText.error = getString(R.string.wrong_format)
                }
            }
            CoordinatesActivity.DDM_FORMAT -> {
                if (latitudeEditText.text.toString().matches(FORMAT_LATITUDE_DDM.toRegex())) {
                    if (longitudeEditText.text.toString().matches(FORMAT_LONGITUDE_DDM.toRegex())) {
                        handleNewLocate(
                            latitudeEditText.text.toString().replaceDDMToNumber(),
                            longitudeEditText.text.toString().replaceDDMToNumber()
                        )
                    } else {
                        longitudeEditText.error = getString(R.string.wrong_format)
                    }
                } else {
                    latitudeEditText.error = getString(R.string.wrong_format)
                }
            }
            CoordinatesActivity.DMS_FORMAT -> {
                if (latitudeEditText.text.toString().matches(FORMAT_LATITUDE_DMS.toRegex())) {
                    if (longitudeEditText.text.toString().matches(FORMAT_LONGITUDE_DMS.toRegex())) {
                        handleNewLocate(
                            latitudeEditText.text.toString().replaceDMSToNumber(),
                            longitudeEditText.text.toString().replaceDMSToNumber()
                        )
                    } else {
                        longitudeEditText.error = getString(R.string.wrong_format)
                    }
                } else {
                    latitudeEditText.error = getString(R.string.wrong_format)
                }
            }
        }
    }

    private fun setupLocationOptions() {
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.newLocationRadioButton -> {
                    lastLocation?.let { lastLocation ->
                        context?.let { context ->
                            setupView(
                                lastLocation.latitude.latitudeCoordinates(context),
                                lastLocation.longitude.longitudeCoordinates(context), true
                            )
                        }
                        setPinOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                    }
                    locationNameTextInput.visibility = View.VISIBLE
                    locationNameSpinner.visibility = View.GONE
                }

                R.id.existingRadioButton -> {
                    locateItem?.let {
                        setPinOnMap(it.getLatLng())
                        context?.let { context ->
                            setupView(
                                locateItem?.latitude.latitudeCoordinates(context),
                                locateItem?.longitude.longitudeCoordinates(context),
                                false
                            )
                        }
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
                context?.let {
                    setupView(
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
            setupLocationOptions()

            getLastLocation()
            retrieveDeployLocations()

            setupInputLocation()
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
                    convertInputLatitude(p0.toString())
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        longitudeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null) {
                    convertInputLongitude(p0.toString())
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    fun convertInputLatitude(latitude: String) {
        when (context.getCoordinatesFormat()) {
            CoordinatesActivity.DD_FORMAT -> {
                if (latitude.matches(FORMAT_LATITUDE_DD.toRegex())) {
                    if (latitude.replaceDDToNumber() > 90.0) {
                        Toast.makeText(
                            context,
                            getString(R.string.latitude_must_between),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        setPinOnMap(
                            LatLng(
                                latitude.replaceDDToNumber(),
                                longitudeEditText.text.toString().replaceDDToNumber()
                            )
                        )
                    }
                } else {
                    latitudeEditText.error = getString(R.string.wrong_format)
                }
            }
            CoordinatesActivity.DDM_FORMAT -> {
                if (latitude.matches(FORMAT_LATITUDE_DDM.toRegex())) {
                    if (latitude.replaceDDMToNumber() > 90.0) {
                        Toast.makeText(
                            context,
                            getString(R.string.latitude_must_between),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        setPinOnMap(
                            LatLng(
                                latitude.replaceDDMToNumber(),
                                longitudeEditText.text.toString().replaceDDMToNumber()
                            )
                        )
                    }
                } else {
                    latitudeEditText.error = getString(R.string.wrong_format)
                }
            }
            CoordinatesActivity.DMS_FORMAT -> {
                if (latitude.matches(FORMAT_LATITUDE_DMS.toRegex())) {
                    if (latitude.replaceDMSToNumber() > 90.0) {
                        Toast.makeText(
                            context,
                            getString(R.string.latitude_must_between),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        setPinOnMap(
                            LatLng(
                                latitude.replaceDMSToNumber(),
                                longitudeEditText.text.toString().replaceDMSToNumber()
                            )
                        )
                    }
                } else {
                    latitudeEditText.error = getString(R.string.wrong_format)
                }
            }
        }
    }

    fun convertInputLongitude(longitude: String) {
        when (context.getCoordinatesFormat()) {
            CoordinatesActivity.DD_FORMAT -> {
                if (longitude.matches(FORMAT_LONGITUDE_DD.toRegex())) {
                    setPinOnMap(
                        LatLng(
                            latitudeEditText.text.toString().replaceDDToNumber(),
                            longitude.replaceDDToNumber()
                        )
                    )
                } else {
                    longitudeEditText.error = getString(R.string.wrong_format)
                }
            }
            CoordinatesActivity.DDM_FORMAT -> {
                if (longitude.matches(FORMAT_LONGITUDE_DDM.toRegex())) {
                    setPinOnMap(
                        LatLng(
                            latitudeEditText.text.toString().replaceDDMToNumber(),
                            longitude.replaceDDMToNumber()
                        )
                    )
                } else {
                    longitudeEditText.error = getString(R.string.wrong_format)
                }
            }
            CoordinatesActivity.DMS_FORMAT -> {
                if (longitude.matches(FORMAT_LONGITUDE_DMS.toRegex())) {
                    setPinOnMap(
                        LatLng(
                            latitudeEditText.text.toString().replaceDMSToNumber(),
                            longitude.replaceDMSToNumber()
                        )
                    )
                } else {
                    longitudeEditText.error = getString(R.string.wrong_format)
                }
            }
        }
    }

    private val locationListener = object : android.location.LocationListener {
        override fun onLocationChanged(p0: Location?) {}
        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
        override fun onProviderEnabled(p0: String?) {}
        override fun onProviderDisabled(p0: String?) {}
    }

    @SuppressLint("MissingPermission")
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
        const val FORMAT_LATITUDE_DD =
            "^(([1-8]?[0-9])(\\.[0-9]{1,6})?|90(\\.0{1,6})?)(\\°)([NSns])\$"
        const val FORMAT_LONGITUDE_DD =
            "^((([1-9]?[0-9]|1[0-7][0-9])(\\.[0-9]{1,6})?)|180(\\.0{1,6})?)(\\°)([EWew])\$"

        const val FORMAT_LATITUDE_DDM =
            "^(([1-8]?[0-9])(\\°[1-5]?[0-9])(\\.[0-9]{1,6})?|90(\\°0{1,6})?|([1-8]?[0-9])(\\°0{1,6})?)(\\')([NSns])\$"
        const val FORMAT_LONGITUDE_DDM =
            "^(([1-9]?[0-9]|1[0-7][0-9])(\\°[1-5]?[0-9])(\\.[0-9]{1,6})?|180(\\°0{1,6})?|([1-9]?[0-9]|1[0-7][0-9])(\\°0{1,6})?)(\\')([EWew])\$"

        const val FORMAT_LATITUDE_DMS =
            "^(([1-8]?[0-9])(\\°[1-5]?[0-8])(\\.[0-9]{1,6})?|90(\\°0{1,6})?|([1-8]?[0-9])(\\°0{1,6})?|([1-8]?[0-9])(\\°59(\\.0{1,6}))?|([1-8]?[0-9])(\\°[1-5]?[0-9])?)((\\'[1-5]?[0-9])?|(\\'[1-5]?[0-9])(\\.[0-9]{1,6})?)(\")([NSns])\$"
        const val FORMAT_LONGITUDE_DMS =
            "^(([1-9]?[0-9]|1[0-7][0-9])(\\°[1-5]?[0-8])(\\.[0-9]{1,6})?|180(\\°0{1,6})?|([1-9]?[0-9]|1[0-7][0-9])(\\°0{1,6})?|([1-9]?[0-9]|1[0-7][0-9])(\\°[1-5]?[0-9])?)((\\'[1-5]?[0-9])?|(\\'[1-5]?[0-9])(\\.[0-9]{1,6})?)(\")([EWew])\$"

        fun newInstance(): LocationFragment {
            return LocationFragment()
        }
    }
}
