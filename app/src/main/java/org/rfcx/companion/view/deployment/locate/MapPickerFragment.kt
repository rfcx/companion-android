package org.rfcx.companion.view.deployment.locate

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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.fragment_map_picker.*
import kotlinx.android.synthetic.main.layout_search_view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.DefaultSetupMap
import org.rfcx.companion.util.latitudeCoordinates
import org.rfcx.companion.util.longitudeCoordinates
import org.rfcx.companion.util.saveLastLocation
import org.rfcx.companion.view.detail.EditLocationActivityListener
import org.rfcx.companion.view.detail.MapPickerProtocol
import java.util.*
import kotlin.concurrent.schedule

class MapPickerFragment :
    Fragment(),
    SearchResultFragment.OnSearchResultListener, OnMapReadyCallback {

    private var mapPickerProtocol: MapPickerProtocol? = null
    private var editLocationActivityListener: EditLocationActivityListener? = null
    private var currentUserLocation: Location? = null
    private var selectedLocation: Location? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    private var nameLocation: String? = null
    private var siteId: Int? = null

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mapPickerProtocol = context as MapPickerProtocol

        try {
            editLocationActivityListener = context as EditLocationActivityListener
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_picker, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        initIntent()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        showLoading(true)

        selectButton.setOnClickListener {
            val currentCameraPosition = map.cameraPosition.target
            analytics?.trackSelectLocationEvent()
            mapPickerProtocol?.onSelectedLocation(
                currentCameraPosition.latitude,
                currentCameraPosition.longitude,
                siteId ?: -1,
                nameLocation ?: ""
            )
        }

        currentLocationButton.setOnClickListener {
            selectedLocation = currentUserLocation
            selectedLocation?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                moveCamera(latLng, DefaultSetupMap.DEFAULT_ZOOM)
                setLatLogLabel(latLng)
            }
        }
        setupSearch()
    }

    private fun initIntent() {
        arguments?.let {
            latitude = it.getDouble(ARG_LATITUDE)
            longitude = it.getDouble(ARG_LONGITUDE)
            altitude = it.getDouble(ARG_ALTITUDE)
            nameLocation = it.getString(ARG_STREAM_NAME)
            siteId = it.getInt(ARG_STREAM_ID)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        fabProgress.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
        currentLocationButton.isEnabled = !isLoading
        currentLocationButton.supportImageTintList =
            if (isLoading) resources.getColorStateList(R.color.gray_30) else resources.getColorStateList(
                R.color.colorPrimary
            )
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        setLatLogLabel(LatLng(latitude, longitude))
        moveCamera(LatLng(latitude, longitude), DefaultSetupMap.DEFAULT_ZOOM)

        if (hasPermissions()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    map.uiSettings.isZoomControlsEnabled = false
                    map.uiSettings.isMyLocationButtonEnabled = false
                    map.isMyLocationEnabled = true
                    context?.let { location?.saveLastLocation(it) }
                    currentUserLocation = location
                    showLoading(false)
                }
        } else {
            requestPermissions()
        }

        map.setOnCameraMoveListener {
            val currentCameraPosition = map.cameraPosition.target
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = currentCameraPosition.latitude
            loc.longitude = currentCameraPosition.longitude
            selectedLocation = loc
            setLatLogLabel(
                LatLng(
                    currentCameraPosition.latitude,
                    currentCameraPosition.longitude
                )
            )
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Float) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
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

    private fun setLatLogLabel(location: LatLng) {
        context?.let {
            val latLng =
                "${location.latitude.latitudeCoordinates(it)}, ${
                    location.longitude.longitudeCoordinates(
                        it
                    )
                }"
            locationTextView.text = latLng
        }
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.MAP_PICKER)
    }

    private fun setupSearch() {
        searchLayoutSearchEditText.visibility = View.GONE
        searchLayoutCardView.setOnClickListener {
            searchLayoutSearchEditText.clearFocus()
        }

        searchLayoutSearchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchLayout.setBackgroundResource(R.color.backgroundColor)
                searchViewActionLeftButton.visibility = View.VISIBLE
                editLocationActivityListener?.hideAppbar()
                showSearchFragment()
            } else {
                searchLayout.setBackgroundResource(R.color.transparent)
                searchViewActionLeftButton.visibility = View.GONE
                searchViewActionRightButton.visibility = View.GONE
                editLocationActivityListener?.showAppbar()
                hideSearchFragment()
            }
        }

        searchViewActionRightButton.setOnClickListener {
            searchLayoutSearchEditText.text = null
        }

        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                searchLayoutSearchEditText.clearFocus()
            }
        }

        searchLayoutSearchEditText.addTextChangedListener(object : TextWatcher {
            var timer: Timer = Timer()
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    searchViewActionRightButton.visibility = View.GONE
                } else {
                    searchViewActionRightButton.visibility = View.VISIBLE
                }
                timer.cancel()
                timer = Timer()
                timer.schedule(200L) {
                    val searchFragment =
                        childFragmentManager.findFragmentByTag(SearchResultFragment.tag) as SearchResultFragment?
                    searchFragment?.search(if (s.isNullOrEmpty()) null else s.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // do nothing
            }
        })

        searchLayoutSearchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm: InputMethodManager? =
                    context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.hideSoftInputFromWindow(searchLayoutSearchEditText.windowToken, 0)
                return@setOnEditorActionListener true
            }
            false
        }

        searchViewActionLeftButton.setOnClickListener {
            clearSearchInputAndHideSoftInput()
        }
    }

    private fun showSearchFragment() {
        childFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.anim.fragment_slide_in_up, 0, 0, R.anim.fragment_slide_out_up)
        }.addToBackStack(SearchResultFragment.tag)
            .replace(
                searchResultListContainer.id,
                SearchResultFragment.newInstance(searchLayoutSearchEditText.text?.toString()),
                SearchResultFragment.tag
            ).commitAllowingStateLoss()
    }

    private fun hideSearchFragment() {
        try {
            if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStack()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearSearchInputAndHideSoftInput() {
        val imm: InputMethodManager? =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(searchLayoutSearchEditText.windowToken, 0)
        searchLayoutSearchEditText.text = null
        searchLayoutSearchEditText.clearFocus()
    }

    companion object {
        private const val TAG = "MapPickerFragment"
        private const val ARG_LATITUDE = "ARG_LATITUDE"
        private const val ARG_LONGITUDE = "ARG_LONGITUDE"
        private const val ARG_ALTITUDE = "ARG_ALTITUDE"
        private const val ARG_STREAM_ID = "ARG_STREAM_ID"
        private const val ARG_STREAM_NAME = "ARG_STREAM_NAME"

        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

        @JvmStatic
        fun newInstance(lat: Double, lng: Double, altitude: Double, id: Int, name: String) =
            MapPickerFragment()
                .apply {
                    arguments = Bundle().apply {
                        putDouble(ARG_LATITUDE, lat)
                        putDouble(ARG_LONGITUDE, lng)
                        putDouble(ARG_ALTITUDE, altitude)
                        putInt(ARG_STREAM_ID, id)
                        putString(ARG_STREAM_NAME, name)
                    }
                }

        fun newInstance(lat: Double, lng: Double, altitude: Double, id: Int) =
            MapPickerFragment()
                .apply {
                    arguments = Bundle().apply {
                        putDouble(ARG_LATITUDE, lat)
                        putDouble(ARG_LONGITUDE, lng)
                        putDouble(ARG_ALTITUDE, altitude)
                        putInt(ARG_STREAM_ID, id)
                    }
                }
    }
}
