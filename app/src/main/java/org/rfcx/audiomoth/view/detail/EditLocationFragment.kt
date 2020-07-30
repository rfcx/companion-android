package org.rfcx.audiomoth.view.detail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.fragment_edit_location.*
import kotlinx.android.synthetic.main.fragment_edit_location.locationNameEditText
import kotlinx.android.synthetic.main.fragment_edit_location.locationValueTextView
import kotlinx.android.synthetic.main.fragment_location.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.convertLatLngLabel
import org.rfcx.audiomoth.view.deployment.locate.LocationFragment

class EditLocationFragment : Fragment(), OnMapReadyCallback {
    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var nameLocation: String? = null

    private var editLocationProtocol: EditLocationProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        editLocationProtocol = context as EditLocationProtocol
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setHideKeyboard()

        locationNameEditText.setText(nameLocation)
        locationValueTextView.text = context?.let { convertLatLngLabel(it, latitude, longitude) }

        changeButton.setOnClickListener {
            editLocationProtocol?.startMapPickerPage(latitude, longitude, nameLocation ?: "")
        }

        saveButton.setOnClickListener {
            editLocationProtocol?.startDetailDeploymentPage(locationNameEditText.text.toString())
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
            nameLocation = it.getString(ARG_LOCATION_NAME)
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        mapboxMap.uiSettings.setAllGesturesEnabled(false)
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false

        mapboxMap.setStyle(Style.OUTDOORS) {
            val latLng = LatLng(latitude, longitude)
            moveCamera(latLng, LocationFragment.DEFAULT_ZOOM)
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Double) {
        mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    private fun View.hideKeyboard() = this.let {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
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
        private const val ARG_LATITUDE = "ARG_LATITUDE"
        private const val ARG_LONGITUDE = "ARG_LONGITUDE"
        private const val ARG_LOCATION_NAME = "ARG_LOCATION_NAME"

        @JvmStatic
        fun newInstance(lat: Double, lng: Double, name: String) =
            EditLocationFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LATITUDE, lat)
                    putDouble(ARG_LONGITUDE, lng)
                    putString(ARG_LOCATION_NAME, name)
                }
            }
    }
}
