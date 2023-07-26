package org.rfcx.companion.view.detail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_edit_location.*
import kotlinx.android.synthetic.main.fragment_edit_location.altitudeEditText
import kotlinx.android.synthetic.main.fragment_edit_location.locationGroupValueTextView
import kotlinx.android.synthetic.main.fragment_edit_location.locationNameEditText
import kotlinx.android.synthetic.main.fragment_edit_location.locationValueTextView
import org.rfcx.companion.R
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.localdb.StreamDb
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.DefaultSetupMap
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.convertLatLngLabel

class EditLocationFragment : Fragment(), OnMapReadyCallback {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val streamDb by lazy { StreamDb(realm) }

    private lateinit var map: GoogleMap

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    private var streamId: Int = -1
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
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        view.viewTreeObserver.addOnGlobalLayoutListener { setOnFocusEditText() }

        setHideKeyboard()

        val stream = editLocationActivityListener?.getStream(streamId)
        val streams = streamDb.getStreams().map { it.name }
        locationNameEditText.setText(stream?.name ?: getString(R.string.none))
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

        locationNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val name = locationNameEditText.text.toString()
                if (streams.contains(name) && (stream?.name ?: getString(R.string.none)) != name) {
                    locationNameTextInput.error = getString(R.string.site_name_exists)
                } else {
                    locationNameTextInput.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun openMapPickerPage() {
        editLocationActivityListener?.startMapPickerPage(
            latitude,
            longitude,
            altitudeEditText.text.toString().toDouble(),
            streamId
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
            streamId = it.getInt(ARG_STREAM_ID)
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.uiSettings.setAllGesturesEnabled(false)

        val latLng = LatLng(latitude, longitude)
        moveCamera(latLng, DefaultSetupMap.DEFAULT_ZOOM)
    }

    private fun moveCamera(latLng: LatLng, zoom: Float) {
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        map.animateCamera(CameraUpdateFactory.zoomTo(zoom))
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
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        } else {
            throw Exception("Request permissions not required before API 23 (should never happen)")
        }
    }

    override fun onResume() {
        super.onResume()
        editLocationActivityListener?.let {
            locationGroupValueTextView.text = it.getStream(streamId).project?.name
        }
    }

    companion object {
        private const val ARG_LATITUDE = "ARG_LATITUDE"
        private const val ARG_LONGITUDE = "ARG_LONGITUDE"
        private const val ARG_ALTITUDE = "ARG_ALTITUDE"
        private const val ARG_STREAM_ID = "ARG_STREAM_ID"
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 34

        @JvmStatic
        fun newInstance(lat: Double, lng: Double, altitude: Double, id: Int) =
            EditLocationFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LATITUDE, lat)
                    putDouble(ARG_LONGITUDE, lng)
                    putDouble(ARG_ALTITUDE, altitude)
                    putInt(ARG_STREAM_ID, id)
                }
            }
    }
}
