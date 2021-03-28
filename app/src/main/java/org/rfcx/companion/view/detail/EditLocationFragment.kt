package org.rfcx.companion.view.detail

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.locationcomponent.getLocationComponentPlugin
import com.mapbox.maps.plugin.scalebar.getScaleBarPlugin
import kotlinx.android.synthetic.main.fragment_edit_location.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.convertLatLngLabel
import org.rfcx.companion.view.deployment.locate.LocationFragment

class EditLocationFragment : Fragment() {
    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    private var nameLocation: String? = null
    private val analytics by lazy { context?.let { Analytics(it) } }

    private var editLocationActivityListener: EditLocationActivityListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        editLocationActivityListener = context as EditLocationActivityListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapBoxView)
        mapView.getMapboxMap().loadStyleUri(Style.OUTDOORS) { style ->
            this.mapboxMap = mapView.getMapboxMap()

            setupScale()
            enableLocationComponent()
            val latLng = Point.fromLngLat(longitude, latitude)
            moveCamera(latLng, LocationFragment.DEFAULT_ZOOM)
        }
        view.viewTreeObserver.addOnGlobalLayoutListener { setOnFocusEditText() }

        setHideKeyboard()

        locationNameEditText.setText(nameLocation)
        altitudeEditText.setText(altitude.toString())
        locationValueTextView.text = context?.let { convertLatLngLabel(it, latitude, longitude) }

        changeButton.setOnClickListener {
            openMapPickerPage()
            analytics?.trackChangeLocationEvent(Screen.EDIT_LOCATION.id)
        }

        viewMapBox.setOnClickListener {
            openMapPickerPage()
            analytics?.trackChangeLocationEvent(Screen.EDIT_LOCATION.id)
        }

        saveButton.setOnClickListener {
            if (locationNameEditText.text.isNullOrBlank()) {
                Toast.makeText(
                    context,
                    getString(R.string.please_fill_information),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                analytics?.trackSaveLocationEvent(Screen.EDIT_LOCATION.id)
                altitude = altitudeEditText.text.toString().toDouble()
                editLocationActivityListener?.updateDeploymentDetail(locationNameEditText.text.toString(), altitude)
            }
        }

        editGroupButton.setOnClickListener {
            analytics?.trackChangeLocationGroupEvent(Screen.EDIT_LOCATION.id)
            editLocationActivityListener?.startLocationGroupPage()
        }
    }

    private fun openMapPickerPage() {
        editLocationActivityListener?.startMapPickerPage(
            latitude,
            longitude,
            altitudeEditText.text.toString().toDouble(),
            locationNameEditText.text.toString()
        )
    }

    private fun setOnFocusEditText() {
        val screenHeight: Int = view?.rootView?.height ?: 0
        val r = Rect()
        view?.getWindowVisibleDisplayFrame(r)
        val keypadHeight: Int = screenHeight - r.bottom
        if (keypadHeight > screenHeight * 0.15) {
            saveButton.visibility = View.GONE
        } else {
            if (saveButton != null) {
                saveButton.visibility = View.VISIBLE
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

    private fun initIntent() {
        arguments?.let {
            latitude = it.getDouble(ARG_LATITUDE)
            longitude = it.getDouble(ARG_LONGITUDE)
            altitude = it.getDouble(ARG_ALTITUDE)
            nameLocation = it.getString(ARG_LOCATION_NAME)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        if (hasPermissions()) {
            mapView.getLocationComponentPlugin().enabled = true
        } else {
            requestPermissions()
        }
    }

    private fun setupScale() {
        val scaleBarPlugin = mapView.getScaleBarPlugin()
        scaleBarPlugin.updateSettings {
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


    private fun moveCamera(latLng: Point, zoom: Double) {
        mapboxMap?.flyTo(CameraOptions.Builder().center(latLng).zoom(zoom).build())
    }

    private fun View.hideKeyboard() = this.let {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
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

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()

        editLocationActivityListener?.let {
            locationGroupValueTextView.text = it.getLocationGroupName()
        }
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
        private const val ARG_LATITUDE = "ARG_LATITUDE"
        private const val ARG_LONGITUDE = "ARG_LONGITUDE"
        private const val ARG_ALTITUDE = "ARG_ALTITUDE"
        private const val ARG_LOCATION_NAME = "ARG_LOCATION_NAME"

        @JvmStatic
        fun newInstance(lat: Double, lng: Double, altitude: Double, name: String) =
            EditLocationFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LATITUDE, lat)
                    putDouble(ARG_LONGITUDE, lng)
                    putDouble(ARG_ALTITUDE, altitude)
                    putString(ARG_LOCATION_NAME, name)
                }
            }
    }
}
