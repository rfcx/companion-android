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
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import kotlinx.android.synthetic.main.fragment_location.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.util.FirestoreResponseCallback
import org.rfcx.audiomoth.view.DeploymentProtocol
import org.rfcx.audiomoth.view.UserListener

class LocationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapView: MapView
    private lateinit var symbolManager: SymbolManager
    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null
    private var locations = ArrayList<String>()
    private var locationsLatLng = ArrayList<LatLng>()
    private var locationLatLng: LatLng? = null
    private var location = ""
    private lateinit var arrayAdapter: ArrayAdapter<String>
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
        context?.let { Mapbox.getInstance(it, MAPBOX_ACCESS_TOKEN) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        deploymentProtocol?.hideCompleteButton()

        finishButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }
    }

    private fun radioCheckedChange() {
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

                    finishButton.isEnabled = locationNameEditText.text.toString().isNotEmpty()

                    locationNameTextInput.visibility = View.VISIBLE
                    locationNameSpinner.visibility = View.GONE
                }

                R.id.existingRadioButton -> {
                    locationLatLng?.let {
                        setPinOnMap(it)
                        setupView(it.latitude.toString(), it.longitude.toString(), false)
                    }
                    locationNameTextInput.visibility = View.GONE
                    locationNameSpinner.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setAdapter() {
        context?.let {
            arrayAdapter =
                ArrayAdapter(it, R.layout.support_simple_spinner_dropdown_item, locations)
        }
        locationNameSpinner.adapter = arrayAdapter
    }

    private fun setLocationSpinner() {
        locationNameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                location = locations[position]
                locationLatLng = locationsLatLng[position]
                setPinOnMap(locationsLatLng[position])

                setupView(
                    locationsLatLng[position].latitude.toString(),
                    locationsLatLng[position].longitude.toString(),
                    false
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun getLocation(documentId: String?) {
        if (documentId.isNullOrEmpty()) {
            newLocationRadioButton.isChecked = true
            existingRadioButton.isEnabled = false
        } else {
            existingRadioButton.isChecked = true

            Firestore().getLocations(documentId,
                object : FirestoreResponseCallback<List<org.rfcx.audiomoth.entity.Location?>?> {
                    override fun onSuccessListener(response: List<org.rfcx.audiomoth.entity.Location?>?) {
                        val locations = ArrayList<String>()
                        response?.map { it ->
                            it?.let { location ->
                                locations.add(location.name)
                                locationsLatLng.add(LatLng(location.latitude, location.longitude))

                                setRecommendLocation()
                            }
                        }
                        arrayAdapter.addAll(locations)
                        arrayAdapter.notifyDataSetChanged()
                    }

                    override fun addOnFailureListener(exception: Exception) {
                        newLocationRadioButton.isChecked = true
                        existingRadioButton.isEnabled = false
                    }
                })
        }
    }

    private fun setRecommendLocation() {
        var minDistance = 51.0
        var latLng: LatLng = locationsLatLng[0]

        locationsLatLng.map {
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = it.latitude
            loc.longitude = it.longitude

            val distance = loc.distanceTo(lastLocation)
            if (distance <= 50.0f) {
                if (minDistance > distance) {
                    minDistance = distance.toDouble()

                    latLng = it
                }
            }
            val position = locationsLatLng.indexOf(latLng)
            locationNameSpinner.setSelection(position)
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
            onLatLngChanged()
            radioCheckedChange()

            getLastLocation()
            getLocation((activity as UserListener).getUserId())
            setAdapter()
            setLocationSpinner()
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
        locationNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null) {
                    finishButton.isEnabled = p0.isNotEmpty()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        latitudeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null) {
                    finishButton.isEnabled = p0.isNotEmpty()
                    if (p0.toString() != "-" && p0.isNotEmpty() && p0.toString() != "." && longitudeEditText.text.toString().isNotEmpty()) {
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
                    finishButton.isEnabled = p0.isNotEmpty()
                    if (p0.toString() != "-" && p0.isNotEmpty() && p0.toString() != "." && latitudeEditText.text.toString().isNotEmpty()) {
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
        const val MAPBOX_ACCESS_TOKEN =
            "pk.eyJ1IjoicmF0cmVlLW9jaG4iLCJhIjoiY2s5Mjk5MDQ3MDYzcDNmbzVnZHd1aXNqaSJ9.UCrMjgGw8zROm_sRlebSGQ"

        fun newInstance(): LocationFragment {
            return LocationFragment()
        }
    }
}
