package org.rfcx.companion.view.deployment.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.SphericalUtil
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.altitudeValue
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.changeProjectTextView
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.coordinatesValueTextView
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.currentLocate
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.locationGroupValueTextView
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.nextButton
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.siteValueTextView
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.viewMapBox
import kotlinx.android.synthetic.main.fragment_detail_deployment_site.withinTextView
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.DefaultSetupMap
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.getLastLocation
import org.rfcx.companion.util.latitudeCoordinates
import org.rfcx.companion.util.longitudeCoordinates
import org.rfcx.companion.util.saveLastLocation
import org.rfcx.companion.util.setFormatLabel
import org.rfcx.companion.util.toLatLng
import org.rfcx.companion.view.deployment.AudioMothDeploymentViewModel
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol
import org.rfcx.companion.view.map.MapCameraUtils
import org.rfcx.companion.view.profile.locationgroup.ProjectActivity

class DetailDeploymentSiteFragment : Fragment(), OnMapReadyCallback {
    private val analytics by lazy { context?.let { Analytics(it) } }
    private val preferences by lazy { context?.let { Preferences.getInstance(it) } }
    private var deploymentProtocol: BaseDeploymentProtocol? = null
    private lateinit var audioMothDeploymentViewModel: AudioMothDeploymentViewModel

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Arguments
    var siteId: Int = -1
    var siteName: String = ""
    var isCreateNew: Boolean = false
    var isUseCurrentLocate: Boolean = false
    var site: Stream? = null
    var fromMapPicker: Boolean = false

    // Location
    private var project: Project? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    private var currentUserLocation: Location? = null
    private var userLocation: Location? = null
    private var pinLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        initIntent()
    }

    private fun setViewModel() {
        audioMothDeploymentViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                requireActivity().application,
                DeviceApiHelper(DeviceApiServiceImpl(requireContext())),
                CoreApiHelper(CoreApiServiceImpl(requireContext())),
                LocalDataHelper()
            )
        ).get(AudioMothDeploymentViewModel::class.java)
    }

    private fun initIntent() {
        arguments?.let {
            siteId = it.getInt(ARG_SITE_ID)
            siteName = it.getString(ARG_SITE_NAME).toString()
            isCreateNew = it.getBoolean(ARG_IS_CREATE_NEW)
            latitude = it.getDouble(ARG_LATITUDE)
            longitude = it.getDouble(ARG_LONGITUDE)
            fromMapPicker = it.getBoolean(ARG_FROM_MAP_PICKER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_detail_deployment_site, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as BaseDeploymentProtocol
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTopBar()
        setViewModel()

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
        updateView()

        changeProjectTextView.setOnClickListener {
            context?.let { it1 ->
                ProjectActivity.startActivity(
                    it1, this.project?.id ?: -1, Screen.DETAIL_DEPLOYMENT_SITE.id
                )
                analytics?.trackChangeLocationGroupEvent(Screen.DETAIL_DEPLOYMENT_SITE.id)
            }
        }

        nextButton.setOnClickListener {
            analytics?.trackSaveLocationEvent(Screen.LOCATION.id)
            this.altitude = currentUserLocation?.altitude ?: 0.0
            getLastLocation()
            if (isCreateNew) {
                createSite()
            } else {
                handleExistLocate()
            }
        }

        currentLocate.setOnClickListener {
            setWithinText()
            isUseCurrentLocate = true
            if (isCreateNew) {
                updateLocationOfNewSite()
            } else {
                site?.let {
                    updateLocationOfExistingSite(
                        currentUserLocation?.latitude ?: it.latitude,
                        currentUserLocation?.longitude ?: it.longitude,
                        currentUserLocation?.altitude ?: it.altitude
                    )
                }
            }
        }

        viewMapBox.setOnClickListener {
            deploymentProtocol?.let {
                getLastLocation()
                val siteLocation = userLocation
                val siteId = if (isCreateNew) -1 else site?.id ?: -1
                it.startMapPicker(
                    siteLocation?.latitude ?: 0.0,
                    siteLocation?.longitude ?: 0.0,
                    siteLocation?.altitude ?: 0.0,
                    siteId,
                    siteName
                )
                it.hideToolbar()
            }
        }
    }

    private fun setupTopBar() {
        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }
    }

    private fun updateLocationOfNewSite() {
        setLatLngToDefault()
        val currentLatLng =
            LatLng(currentUserLocation?.latitude ?: 0.0, currentUserLocation?.longitude ?: 0.0)
        createSiteSymbol(currentLatLng)
        moveCamera(currentLatLng, DefaultSetupMap.DEFAULT_ZOOM)
    }

    private fun setLatLngToDefault() {
        latitude = 0.0
        longitude = 0.0
    }

    private fun updateLocationOfExistingSite(
        latitude: Double,
        longitude: Double,
        altitude: Double
    ) {
        setLatLngToDefault()
        var locate = Stream()
        site?.let {
            locate = Stream(
                id = it.id,
                serverId = it.serverId,
                name = it.name,
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                lastDeploymentId = it.lastDeploymentId,
                syncState = it.syncState,
                project = getProject(it.project?.id ?: 0),
                deployments = it.deployments
            )
            createSiteSymbol(locate.getLatLng())
            moveCamera(locate.getLatLng(), DefaultSetupMap.DEFAULT_ZOOM)
        }
        site = locate
    }

    private fun createSite() {
        val name = siteValueTextView.text.toString()
        userLocation?.let {
            val locate = Stream(
                name = name,
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = altitude,
                project = getProject(project?.id ?: -1)
            )
            deploymentProtocol?.setDeployLocation(locate, false)
            deploymentProtocol?.nextStep()
        }
    }

    private fun handleExistLocate() {
        site?.let {
            val locate = Stream(
                id = it.id,
                serverId = it.serverId,
                name = it.name,
                latitude = if (userLocation?.latitude != 0.0) userLocation?.latitude
                    ?: it.latitude else it.latitude,
                longitude = if (userLocation?.longitude != 0.0) userLocation?.longitude
                    ?: it.longitude else it.longitude,
                altitude = currentUserLocation?.altitude ?: it.altitude,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                lastDeploymentId = it.lastDeploymentId,
                syncState = it.syncState,
                project = getProject(it.project?.id ?: 0),
                deployments = it.deployments
            )
            deploymentProtocol?.setDeployLocation(locate, true)
            deploymentProtocol?.nextStep()
        }
    }

    private fun getProject(id: Int): Project {
        return deploymentProtocol?.getProject(id) ?: Project()
    }

    private fun getLastLocation() {
        if (latitude != 0.0 && longitude != 0.0) {
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = latitude
            loc.longitude = longitude
            userLocation = loc
        } else if (isCreateNew) {
            userLocation = currentUserLocation
                ?: deploymentProtocol?.getCurrentLocation() // get new current location
        } else {
            site?.let {
                val loc = Location(LocationManager.GPS_PROVIDER)
                loc.latitude = it.latitude
                loc.longitude = it.longitude
                userLocation = loc
            }
        }
    }

    private fun updateView() {
        if (!isCreateNew) site = site ?: audioMothDeploymentViewModel.getStreamById(siteId)
        if (currentUserLocation == null) {
            currentUserLocation = context?.getLastLocation()
        }
        if (latitude != 0.0 && longitude != 0.0) {
            val alt = currentUserLocation?.altitude
            setLatLngLabel(LatLng(latitude, longitude), alt ?: 0.0)
            pinLocation = LatLng(latitude, longitude)
        } else if (isCreateNew) {
            currentUserLocation?.let {
                setLatLngLabel(it.toLatLng(), it.altitude)
                pinLocation = it.toLatLng()
            }
        } else {
            val alt = currentUserLocation?.altitude
            site?.let {
                setLatLngLabel(it.toLatLng(), alt ?: 0.0)
                pinLocation = it.toLatLng()
            }
            locationGroupValueTextView.text = site?.project?.name ?: getString(R.string.none)
        }
        siteValueTextView.text = siteName
        changeProjectTextView.visibility = View.GONE
    }

    private fun setLatLngLabel(location: LatLng, altitude: Double) {
        context?.let {
            val latLng = "${location.latitude.latitudeCoordinates(it)}, ${location.longitude.longitudeCoordinates(it)}"
            coordinatesValueTextView.text = latLng
            altitudeValue.text = altitude.setFormatLabel()
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.uiSettings.setAllGesturesEnabled(false)
        setPinOnMap()

        if (hasPermissions()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                map.isMyLocationEnabled = true
                context?.let { location?.saveLastLocation(it) }
                currentUserLocation = location
            }
        } else {
            requestPermissions()
        }
    }

    private fun setPinOnMap() {
        val curLoc = context?.getLastLocation()?.toLatLng() ?: LatLng(0.0, 0.0)
        if (latitude != 0.0 && longitude != 0.0) {
            val latLng = LatLng(latitude, longitude)
            moveCamera(curLoc, latLng, DefaultSetupMap.DEFAULT_ZOOM)
            createSiteSymbol(latLng)
            setCheckboxForResumeDeployment(curLoc, latLng)
            pinLocation = latLng
        } else if (!isCreateNew) {
            site = audioMothDeploymentViewModel.getStreamById(siteId)
            site?.let { locate ->
                val latLng = locate.getLatLng()
                moveCamera(curLoc, latLng, DefaultSetupMap.DEFAULT_ZOOM)
                setCheckboxForResumeDeployment(
                    curLoc, latLng
                )
                createSiteSymbol(latLng)
                pinLocation = latLng
            }
        } else {
            moveCamera(curLoc, DefaultSetupMap.DEFAULT_ZOOM)
            createSiteSymbol(curLoc)
            setWithinText()
            pinLocation = curLoc
        }
    }

    private fun createSiteSymbol(latLng: LatLng) {
        map.clear()
        map.addMarker(
            MarkerOptions().title("").position(latLng)
                .icon(bitmapFromVector(requireContext(), R.drawable.ic_pin_map))
        )
    }

    private fun hasPermissions(): Boolean {
        val permissionState = context?.let {
            ActivityCompat.checkSelfPermission(
                it, Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity?.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSIONS_REQUEST_CODE
            )
        } else {
            throw Exception("Request permissions not required before API 23 (should never happen)")
        }
    }

    private fun moveCamera(userPosition: LatLng, nearestSite: LatLng?, zoom: Float) {
        map.moveCamera(
            MapCameraUtils.calculateLatLngForZoom(
                userPosition, nearestSite, zoom
            )
        )
    }

    private fun moveCamera(latLng: LatLng, zoom: Float) {
        map.moveCamera(MapCameraUtils.calculateLatLngForZoom(latLng, null, zoom))
    }

    private fun setCheckboxForResumeDeployment(curLoc: LatLng, target: LatLng) {
        val distance = SphericalUtil.computeDistanceBetween(curLoc, target)
        if (distance <= 20) {
            setWithinText()
        } else {
            setNotWithinText(distance.setFormatLabel())
        }
    }

    private fun setWithinText() {
        withinTextView.text = getString(R.string.within)
        withinTextView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_checklist_passed, 0, 0, 0
        )
    }

    private fun setNotWithinText(distance: String) {
        withinTextView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_checklist_cross, 0, 0, 0
        )
        withinTextView.text = getString(R.string.more_than, distance)
    }

    override fun onResume() {
        super.onResume()

        val projectId = preferences?.getInt(Preferences.SELECTED_PROJECT) ?: -1
        val selectedProject = audioMothDeploymentViewModel.getProjectById(projectId)
        val editProjectId = preferences?.getInt(Preferences.EDIT_PROJECT) ?: -1
        val selectedEditProject = audioMothDeploymentViewModel.getProjectById(editProjectId)

        this.project = selectedEditProject ?: selectedProject
        locationGroupValueTextView.text = this.project?.name
    }

    private fun bitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable: Drawable = ContextCompat.getDrawable(context, vectorResId)!!
        vectorDrawable.setBounds(
            0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight
        )
        val bitmap: Bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas: Canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    companion object {
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

        private const val ARG_SITE_ID = "ARG_SITE_ID"
        private const val ARG_SITE_NAME = "ARG_SITE_NAME"
        private const val ARG_IS_CREATE_NEW = "ARG_IS_CREATE_NEW"
        private const val ARG_LATITUDE = "ARG_LATITUDE"
        private const val ARG_LONGITUDE = "ARG_LONGITUDE"
        private const val ARG_FROM_MAP_PICKER = "ARG_FROM_MAP_PICKER"

        const val PROPERTY_MARKER_IMAGE = "marker.image"

        @JvmStatic
        fun newInstance() = DetailDeploymentSiteFragment()

        fun newInstance(id: Int, name: String, isCreateNew: Boolean = false) =
            DetailDeploymentSiteFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SITE_ID, id)
                    putString(ARG_SITE_NAME, name)
                    putBoolean(ARG_IS_CREATE_NEW, isCreateNew)
                }
            }

        fun newInstance(
            lat: Double, lng: Double,
            siteId: Int,
            siteName: String,
            fromMapPicker: Boolean
        ) = DetailDeploymentSiteFragment().apply {
            arguments = Bundle().apply {
                putDouble(ARG_LATITUDE, lat)
                putDouble(ARG_LONGITUDE, lng)
                putInt(ARG_SITE_ID, siteId)
                putString(ARG_SITE_NAME, siteName)
                putBoolean(ARG_IS_CREATE_NEW, siteId == -1)
                putBoolean(ARG_FROM_MAP_PICKER, fromMapPicker)
            }
        }

        fun newInstance(lat: Double, lng: Double, siteId: Int, siteName: String) =
            DetailDeploymentSiteFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LATITUDE, lat)
                    putDouble(ARG_LONGITUDE, lng)
                    putInt(ARG_SITE_ID, siteId)
                    putString(ARG_SITE_NAME, siteName)
                    putBoolean(ARG_IS_CREATE_NEW, siteId == -1)
                }
            }
    }
}
