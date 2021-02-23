package org.rfcx.companion.view.detail

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.pluginscalebar.ScaleBarOptions
import com.mapbox.pluginscalebar.ScaleBarPlugin
import kotlinx.android.synthetic.main.fragment_edit_location.*
import kotlinx.android.synthetic.main.fragment_edit_location.altitudeEditText
import kotlinx.android.synthetic.main.fragment_edit_location.locationGroupValueTextView
import kotlinx.android.synthetic.main.fragment_edit_location.locationNameEditText
import kotlinx.android.synthetic.main.fragment_edit_location.locationValueTextView
import kotlinx.android.synthetic.main.fragment_edit_location.pinDeploymentImageView
import org.rfcx.companion.R
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.convertLatLngLabel
import org.rfcx.companion.view.deployment.locate.LocationFragment

class EditLocationFragment : Fragment(), OnMapReadyCallback {
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
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
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

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        mapboxMap.uiSettings.setAllGesturesEnabled(false)
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false

        mapboxMap.setStyle(Style.OUTDOORS) {
            setupScale()
            val latLng = LatLng(latitude, longitude)
            moveCamera(latLng, LocationFragment.DEFAULT_ZOOM)
        }
    }

    private fun setupScale() {
        val scaleBarPlugin = ScaleBarPlugin(mapView, mapboxMap!!)
        val options = ScaleBarOptions(requireContext())
        options.setMarginTop(R.dimen.legend_top_margin)
        scaleBarPlugin.create(options)
    }

    private fun moveCamera(latLng: LatLng, zoom: Double) {
        mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    private fun changePinColorByGroup(group: String) {
        val locationGroup = editLocationActivityListener?.getLocationGroup(group)
        val color = locationGroup?.color
        val pinDrawable = pinDeploymentImageView
        if (color != null && color.isNotEmpty() && group != getString(R.string.none)) {
            pinDrawable.setColorFilter(color.toColorInt())
        } else {
            pinDrawable.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        }
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

        editLocationActivityListener?.let {
            changePinColorByGroup(
                it.getLocationGroupName()
            )
            locationGroupValueTextView.text = it.getLocationGroupName()
        }
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
