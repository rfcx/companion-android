package org.rfcx.companion.view.map

//import com.mapbox.android.core.location.*
//import com.mapbox.geojson.Feature
//import com.mapbox.geojson.FeatureCollection
//import com.mapbox.geojson.LineString
//import com.mapbox.geojson.Point
//import com.mapbox.mapboxsdk.Mapbox
//import com.mapbox.mapboxsdk.annotations.BubbleLayout
//import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
//import com.mapbox.mapboxsdk.geometry.LatLng
//import com.mapbox.mapboxsdk.geometry.LatLngBounds
//import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
//import com.mapbox.mapboxsdk.location.LocationComponentOptions
//import com.mapbox.mapboxsdk.location.modes.CameraMode
//import com.mapbox.mapboxsdk.location.modes.RenderMode
//import com.mapbox.mapboxsdk.maps.MapView
//import com.mapbox.mapboxsdk.maps.MapboxMap
//import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
//import com.mapbox.mapboxsdk.maps.Style
//import com.mapbox.mapboxsdk.style.expressions.Expression.*
//import com.mapbox.mapboxsdk.style.layers.CircleLayer
//import com.mapbox.mapboxsdk.style.layers.LineLayer
//import com.mapbox.mapboxsdk.style.layers.Property.*
//import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
//import com.mapbox.mapboxsdk.style.layers.SymbolLayer
//import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
//import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
//import com.mapbox.mapboxsdk.utils.BitmapUtils
import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.google.maps.android.clustering.ClusterManager
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.layout_deployment_window_info.view.*
import kotlinx.android.synthetic.main.layout_map_window_info.view.*
import kotlinx.android.synthetic.main.layout_search_view.*
import org.rfcx.companion.MainActivityListener
import org.rfcx.companion.MainViewModel
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.*
import org.rfcx.companion.localdb.StreamDb
import org.rfcx.companion.localdb.TrackingDb
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import org.rfcx.companion.view.detail.DeploymentDetailActivity
import org.rfcx.companion.view.profile.locationgroup.ProjectActivity
import org.rfcx.companion.view.profile.locationgroup.ProjectAdapter
import org.rfcx.companion.view.profile.locationgroup.ProjectListener
import org.rfcx.companion.view.unsynced.UnsyncedWorksActivity
import java.util.*

class MapFragment : Fragment(), ProjectListener, OnMapReadyCallback,
    GoogleMap.OnInfoWindowClickListener, (Stream, Boolean) -> Unit {

    // Google map
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mClusterManager: ClusterManager<MarkerItem>


    /* Old code TODO: #Tree delete this line */
    private lateinit var mainViewModel: MainViewModel

    // map
//    private lateinit var mapView: MapView
//    private var mapboxMap: MapboxMap? = null
//    private var locationEngine: LocationEngine? = null
//    private var mapSource: GeoJsonSource? = null
//    private var lineSource: GeoJsonSource? = null
//    private var mapFeatures: FeatureCollection? = null

    // database manager
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val trackingDb by lazy { TrackingDb(realm) }
    private val streamDb by lazy { StreamDb(realm) }

    // data
    private var streams = listOf<Stream>()
    private var projects = listOf<Project>()
    private var lastSyncingInfo: SyncInfo? = null

    private var deploymentMarkers = listOf<MapMarker.DeploymentMarker>()
    private var unsyncedDeploymentCount = 0
    private var streamMarkers = listOf<MapMarker>()

    private lateinit var deploymentWorkInfoLiveData: LiveData<List<WorkInfo>>
    private lateinit var downloadStreamsWorkInfoLiveData: LiveData<List<WorkInfo>>

    private val locationPermissions by lazy { activity?.let { LocationPermissions(it) } }
    private var listener: MainActivityListener? = null

    private var isFirstTime = true
    private var currentUserLocation: Location? = null

    private val analytics by lazy { context?.let { Analytics(it) } }
    private val firebaseCrashlytics by lazy { Crashlytics() }

    private val handler: Handler = Handler()

    // for animate line string
//    private var routeCoordinateList = listOf<Point>()
    private var routeIndex = 0

    //    private var markerLinePointList = arrayListOf<ArrayList<Point>>()
    private var currentAnimator: Animator? = null

    //    private var queue = arrayListOf<List<Point>>()
    private var queueColor = arrayListOf<String>()
    private var queuePivot = 0

    private var currentMarkId = ""

    private var screen = ""

    private val siteAdapter by lazy { SiteAdapter(this) }
    private var adapterOfSearchSite: List<SiteWithLastDeploymentItem>? = null

    private val locationGroupAdapter by lazy { ProjectAdapter(this) }

//    private val mapboxLocationChangeCallback =
//        object : LocationEngineCallback<LocationEngineResult> {
//            override fun onSuccess(result: LocationEngineResult?) {
//                if (activity != null) {
//                    val location = result?.lastLocation
//                    location ?: return
//                    context?.let { location.saveLastLocation(it) }
//
//                    mapboxMap?.let {
//                        this@MapFragment.currentUserLocation = location
//                    }
//                    if (isFirstTime && streams.isNotEmpty()) {
//                        moveCameraOnStartWithProject()
//                        isFirstTime = false
//                    }
//                }
//            }
//
//            override fun onFailure(exception: Exception) {
//                Log.e(MapFragment.tag, exception.localizedMessage ?: "empty localizedMessage")
//            }
//        }

    // observer
    private val deploymentWorkInfoObserve = Observer<List<WorkInfo>> {
        val currentWorkStatus = it?.getOrNull(0)
        if (currentWorkStatus != null) {
            when (currentWorkStatus.state) {
                WorkInfo.State.RUNNING -> updateSyncInfo(SyncInfo.Uploading, false)
                WorkInfo.State.SUCCEEDED -> updateSyncInfo(SyncInfo.Uploaded, false)
                else -> updateSyncInfo(isSites = false)
            }
        }
    }

    private val downloadStreamsWorkInfoObserve = Observer<List<WorkInfo>> {
        val currentWorkStatus = it?.getOrNull(0)
        if (currentWorkStatus != null) {
            when (currentWorkStatus.state) {
                WorkInfo.State.RUNNING -> updateSyncInfo(SyncInfo.Uploading, true)
                WorkInfo.State.SUCCEEDED -> {
                    updateSyncInfo(SyncInfo.Uploaded, true)
                    mainViewModel.updateProjectBounds()
//                    mainViewModel.updateStatusOfflineMap()
                }

                else -> updateSyncInfo(isSites = true)
            }
        }
    }

    private val getProjectsFromRemoteObserver = Observer<Resource<List<Project>>> {
        when (it.status) {
            Status.LOADING -> {
            }

            Status.SUCCESS -> {
                mainViewModel.updateProjectBounds()
                projectSwipeRefreshView.isRefreshing = false

                this.projects = mainViewModel.getProjectsFromLocal()
                locationGroupAdapter.items = listOf()
                locationGroupAdapter.items = this.projects
                locationGroupAdapter.notifyDataSetChanged()

                combinedData()
//                mainViewModel.updateStatusOfflineMap()
            }

            Status.ERROR -> {
                combinedData()
                projectSwipeRefreshView.isRefreshing = false
                showToast(it.message ?: getString(R.string.error_has_occurred))
            }
        }
    }

    private val getDeploymentMarkerObserver = Observer<List<MapMarker.DeploymentMarker>> {
        deploymentMarkers = it ?: listOf()
        combinedData()
    }

    private val getStreamMarkerObserver = Observer<List<MapMarker>> {
        streamMarkers = it ?: listOf()
        combinedData()
    }

    private val getStreamObserver = Observer<List<Stream>> {
        streams = it ?: listOf()
        combinedData()
    }

    private val getUnsyncedDeploymentsObserver = Observer<Int> {
        unsyncedDeploymentCount = it ?: 0
        updateUnsyncedCount(unsyncedDeploymentCount)
    }


    /* New code TODO: #Tree delete this line */
    override fun onMapReady(p0: GoogleMap) {
        p0.setOnInfoWindowClickListener(this)
        map = p0
        mainViewModel.retrieveLocations()
        mainViewModel.fetchProjects()
        setUpClusterer()
        setupSearch()
        setObserver()

        if (locationPermissions?.allowed() == false) {
            locationPermissions?.check { /* do nothing */ }
        } else {
            enableMyLocation()
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0)))
                map.uiSettings.isZoomControlsEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = false
                context?.let { location?.saveLastLocation(it) }
                currentUserLocation = location
            }

    }

    private fun setUpClusterer() {
        // Create the ClusterManager class and set the custom renderer.
        mClusterManager = ClusterManager<MarkerItem>(requireContext(), map)
        mClusterManager.renderer =
            MarkerRenderer(
                requireContext(),
                map,
                mClusterManager
            )

        // Set custom info window adapter
        mClusterManager.markerCollection.setInfoWindowAdapter(InfoWindowAdapter(requireContext()))

        map.setOnCameraIdleListener(mClusterManager)
        map.setOnMarkerClickListener(mClusterManager)
        mClusterManager.markerCollection.setInfoWindowAdapter(InfoWindowAdapter(requireContext()))
        map.setInfoWindowAdapter(mClusterManager.markerManager)
        combinedData()

        // can re-cluster when zooming in and out.
        map.setOnCameraIdleListener {
            mClusterManager.onCameraIdle()
        }
    }

    override fun onInfoWindowClick(p0: Marker) {
        if (p0.snippet == null) return
        val isDeployment = isDeployment(p0.snippet!!)

        if (isDeployment) {
            val data = Gson().fromJson(p0.snippet, MapMarker.DeploymentMarker::class.java)

            context?.let {
                firebaseCrashlytics.setCustomKey(
                    CrashlyticsKey.OnClickSeeDetail.key,
                    "Site: ${data.locationName}, Project: ${data.projectName}"
                )
                DeploymentDetailActivity.startActivity(it, data.id)
                analytics?.trackSeeDetailEvent()
            }
        } else {
            return
        }
    }

    private fun isDeployment(data: String): Boolean {
        return data.contains("deploymentKey")
    }

    private fun setMarker(mapMarker: List<MapMarker>) {
        mapMarker.map {
            when (it) {
                is MapMarker.DeploymentMarker -> {
                    setMarker(it)
                }

                is MapMarker.SiteMarker -> {
                    setMarker(it)
                }
            }
        }
    }

    private fun setMarker(data: MapMarker.SiteMarker) {
        // Add Marker
        val latlng = LatLng(data.latitude, data.longitude)
        val item = MarkerItem(data.latitude, data.longitude, data.name, Gson().toJson(data))
        mClusterManager.addItem(item)
        mClusterManager.cluster()

        // Move Camera
        map.moveCamera(CameraUpdateFactory.newLatLng(latlng))
        map.animateCamera(CameraUpdateFactory.zoomTo(13.0f));
    }

    private fun setMarker(data: MapMarker.DeploymentMarker) {
        // Add Marker
        val latlng = LatLng(data.latitude, data.longitude)
        val item = MarkerItem(data.latitude, data.longitude, data.locationName, Gson().toJson(data))
        mClusterManager.addItem(item)
        mClusterManager.cluster()

        // Move Camera
        map.moveCamera(CameraUpdateFactory.newLatLng(latlng))
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.listener = context as MainActivityListener
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationPermissions?.handleActivityResult(requestCode, resultCode)
        screen = data?.getStringExtra(ProjectActivity.EXTRA_SCREEN) ?: ""
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissions?.handleRequestResult(requestCode, grantResults)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

//        context?.let { Mapbox.getInstance(it, getString(R.string.mapbox_token)) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        mapView = view.findViewById(R.id.mapView)
//        mapView.onCreate(savedInstanceState)
//        mapView.getMapAsync(this)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)



        setViewModel()

        fetchJobSyncing()
        showSearchBar(false)
        hideLabel()

        context?.let { setTextTrackingButton(LocationTrackingManager.isTrackingOn(it)) }
        projectNameTextView.text =
            if (listener?.getProjectName() != getString(R.string.none)) listener?.getProjectName() else getString(
                R.string.projects
            )
        searchLayoutSearchEditText.hint = getString(R.string.site_name_hint)

        currentLocationButton.setOnClickListener {
            currentUserLocation?.let {
                map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude)))
            }
        }

        zoomOutButton.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.zoomOut())
        }

        zoomInButton.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.zoomIn())
        }

        projectNameTextView.setOnClickListener {
            setOnClickProjectName()
        }

        unSyncedDpNumber.setOnClickListener {
            if (unsyncedDeploymentCount != 0) {
                UnsyncedWorksActivity.startActivity(requireContext())
            }
        }

        projectSwipeRefreshView.apply {
            setOnRefreshListener {
                mainViewModel.fetchProjects()
                isRefreshing = true
            }
            setColorSchemeResources(R.color.colorPrimary)
        }

        siteSwipeRefreshView.apply {
            setOnRefreshListener {
                mainViewModel.retrieveLocations()
                isRefreshing = false
            }
            setColorSchemeResources(R.color.colorPrimary)
        }

        iconOpenProjectList.setOnClickListener {
            setOnClickProjectName()
        }

        siteRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = siteAdapter
        }

        projectRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = locationGroupAdapter
            locationGroupAdapter.screen = Screen.MAP.id
        }
    }

    private fun setViewModel() {
        mainViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                requireActivity().application,
                DeviceApiHelper(DeviceApiServiceImpl(requireContext())),
                CoreApiHelper(CoreApiServiceImpl(requireContext())),
                LocalDataHelper()
            )
        ).get(MainViewModel::class.java)
    }

    private fun setOnClickProjectName() {
        val state = listener?.getBottomSheetState() ?: 0
        if (state == BottomSheetBehavior.STATE_EXPANDED && searchLayout.visibility != View.VISIBLE) {
            clearFeatureSelected()
            listener?.hideBottomSheet()
        }

        if (projectRecyclerView.visibility == View.VISIBLE) {
            projectRecyclerView.visibility = View.GONE
            projectSwipeRefreshView.visibility = View.GONE
            searchButton.visibility = View.VISIBLE
            trackingLayout.visibility = View.VISIBLE
            showButtonOnMap()
            listener?.showBottomAppBar()
        } else {
            projectRecyclerView.visibility = View.VISIBLE
            projectSwipeRefreshView.visibility = View.VISIBLE
            showSearchBar(false)
            searchButton.visibility = View.GONE
            trackingLayout.visibility = View.GONE
            hideButtonOnMap()
            listener?.hideBottomAppBar()
        }

        if (siteRecyclerView.visibility == View.VISIBLE) {
            searchLayout.visibility = View.GONE
            hideLabel()
            searchLayoutSearchEditText.text = null
        }
    }

    private fun showLabel(isNotFound: Boolean) {
        if (siteRecyclerView.visibility == View.VISIBLE && projectRecyclerView.visibility != View.VISIBLE) {
            showLabelLayout.visibility = View.VISIBLE
            notHaveSiteTextView.visibility = if (isNotFound) View.GONE else View.VISIBLE
            notHaveResultTextView.visibility = if (isNotFound) View.VISIBLE else View.GONE
        }
    }

    private fun hideLabel() {
        showLabelLayout.visibility = View.GONE
    }

    private fun setupSearch() {
        searchButton.setOnClickListener {
            showSearchBar(true)
        }

        searchViewActionRightButton.setOnClickListener {
            if (searchLayoutSearchEditText.text.isNullOrBlank()) {
                showSearchBar(false)
                it.hideKeyboard()
            } else {
                searchLayoutSearchEditText.text = null
            }
        }

        searchLayoutSearchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                context?.let {
                    val text = s.toString().lowercase(Locale.getDefault())
                    val filtered = adapterOfSearchSite?.filter { site ->
                        site.stream.name.lowercase(
                            Locale.getDefault()
                        ).contains(text)
                    }
                    if (filtered.isNullOrEmpty()) showLabel(true) else hideLabel()
                    siteAdapter.setFilter(filtered)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        trackingLayout.setOnClickListener {
            if (locationPermissions?.allowed() == false) {
                locationPermissions?.check { /* do nothing */ }
            } else {
                onTrackingClicked()
            }
        }
    }

    private fun onTrackingClicked() {
        context?.let { context ->
            if (LocationTrackingManager.isTrackingOn(context)) {
                setLocationTrackingService(context, false)
            } else {
                val tracking = mainViewModel.getFirstTracking()
                if (tracking != null) {
                    val time = tracking.stopAt?.time?.plus(WITHIN_TIME * 60000)
                    time?.let {
                        if (it > Date().time) {
                            setLocationTrackingService(context, true)
                        } else {
                            mainViewModel.deleteTracking(1, context)
                            setLocationTrackingService(context, true)
                        }
                    }
                } else {
                    setLocationTrackingService(context, true)
                }
            }
        }
    }

    fun showSearchBar(show: Boolean) {
        searchLayout.visibility = if (show) View.VISIBLE else View.INVISIBLE
        siteRecyclerView.visibility = if (show) View.VISIBLE else View.INVISIBLE
        siteSwipeRefreshView.visibility = if (show) View.VISIBLE else View.INVISIBLE
        searchViewActionRightButton.visibility = if (show) View.VISIBLE else View.INVISIBLE
        searchButton.visibility = if (show) View.GONE else View.VISIBLE
        trackingLayout.visibility = if (show) View.GONE else View.VISIBLE

        if (show) {
            setSearchView()
            searchLayout.setBackgroundResource(R.color.backgroundColorSite)
        } else {
            searchLayoutSearchEditText.text = null
            searchLayout.setBackgroundResource(R.color.transparent)

            hideLabel()
            siteRecyclerView.visibility = View.GONE
            listener?.showBottomAppBar()
            listener?.clearFeatureSelectedOnMap()
        }
    }

    private fun setSearchView() {
        hideButtonOnMap()
        val state = listener?.getBottomSheetState() ?: 0
        if (state == BottomSheetBehavior.STATE_EXPANDED) {
            listener?.hideBottomSheetAndBottomAppBar()
        } else {
            listener?.hideBottomAppBar()
        }

        if (siteAdapter.itemCount == 0) {
            showLabel(false)
        } else {
            hideLabel()
        }
    }

    private fun setLocationTrackingService(context: Context, isOn: Boolean) {
        setTextTrackingButton(isOn)
        LocationTrackingManager.set(context, isOn)
    }

    private fun setTextTrackingButton(isOn: Boolean) {
        context?.let { context ->
            if (isOn) {
                trackingImageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_tracking_on
                    )
                )
                startCounting()
            } else {
                handler.removeCallbacks(run)
                trackingTextView.text = getString(R.string.track)
                trackingImageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_tracking_off
                    )
                )
            }
        }
    }

    private fun startCounting() {
        handler.post(run)
    }

    private val run: Runnable = object : Runnable {
        override fun run() {
            context?.let {
                trackingTextView.text = "${
                    LocationTrackingManager.getDistance(trackingDb).setFormatLabel()
                }  ${LocationTrackingManager.getOnDutyTimeMinute(it)} min"
            }
            handler.postDelayed(this, 20 * 1000L)
        }
    }

//    override fun onMapReady(mapboxMap: MapboxMap) {
//        this.mapboxMap = mapboxMap
//        mapboxMap.uiSettings.isAttributionEnabled = false
//        mapboxMap.uiSettings.isLogoEnabled = false
//        mapboxMap.uiSettings.setCompassMargins(0, 1350, 900, 0)
//
//        mainViewModel.fetchProjects()
//        mainViewModel.retrieveLocations()
//
//        mapboxMap.setStyle(Style.OUTDOORS) {
//            checkThenAccquireLocation(it)
//            setupSources(it)
//            setupImages(it)
//            setupMarkerLayers(it)
//            setupSearch()
//            setupWindowInfo(it)
//
//            mapboxMap.addOnMapClickListener { latLng ->
//                val screenPoint = mapboxMap.projection.toScreenLocation(latLng)
//                val features = mapboxMap.queryRenderedFeatures(screenPoint, WINDOW_MARKER_ID)
//                val symbolScreenPoint = mapboxMap.projection.toScreenLocation(latLng)
//                if (features.isNotEmpty()) {
//                    handleClickCallout(features[0])
//                } else {
//                    handleClickIcon(symbolScreenPoint)
//                }
//            }
//        }
//    }

//    private fun setupSources(style: Style) {
//        mapSource =
//            GeoJsonSource(
//                SOURCE_DEPLOYMENT,
//                FeatureCollection.fromFeatures(listOf()),
//                GeoJsonOptions()
//                    .withCluster(true)
//                    .withClusterMaxZoom(25)
//                    .withClusterRadius(30)
//                    .withClusterProperty(
//                        PROPERTY_CLUSTER_TYPE,
//                        sum(accumulated(), get(PROPERTY_CLUSTER_TYPE)),
//                        switchCase(
//                            any(
//                                eq(
//                                    get(PROPERTY_DEPLOYMENT_MARKER_IMAGE),
//                                    Pin.PIN_GREEN
//                                )
//                            ),
//                            literal(1),
//                            literal(0)
//                        )
//                    )
//            )
//
//        lineSource = GeoJsonSource(SOURCE_LINE)
//
//        style.addSource(mapSource!!)
//        style.addSource(lineSource!!)
//    }

    fun clearFeatureSelected() {
//        if (this.mapFeatures?.features() != null) {
//            val features = this.mapFeatures!!.features()
//            features?.forEach { setFeatureSelectState(it, false) }
//        }
    }

//    private fun setFeatureSelectState(feature: Feature, selectedState: Boolean) {
//        feature.properties()?.let {
//            it.addProperty(PROPERTY_DEPLOYMENT_SELECTED, selectedState)
//            refreshSource()
//        }
//    }

//    private fun setDeploymentDetail(feature: Feature) {
//        val markerId = feature.getProperty(
//            PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID
//        ).asString
//
//        val windowInfoImages = hashMapOf<String, Bitmap>()
//        val inflater = LayoutInflater.from(context)
//
//        val layout =
//            inflater.inflate(R.layout.layout_deployment_window_info, null) as BubbleLayout
//        val id = feature.getStringProperty(PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID) ?: ""
//        val title = feature.getStringProperty(PROPERTY_DEPLOYMENT_MARKER_TITLE)
//
//        var stream: Stream? = null
//        val deploymentId = feature.getStringProperty(PROPERTY_DEPLOYMENT_MARKER_DEPLOYMENT_ID)
//        deploymentId?.let {
//            val deployment = mainViewModel.getDeploymentById(it.toInt())
//            val type = deployment?.device ?: Device.AUDIOMOTH.value
//            layout.deploymentTypeName.text = "type: ${type.toUpperCase()}"
//
//            stream = deployment?.stream
//            val streamId = stream?.serverId
//            layout.deploymentStreamId.visibility = View.VISIBLE
//            layout.deploymentStreamId.text = "id: $streamId"
//        }
//        layout.deploymentSiteTitle.text = title
//        val projectName = feature.getStringProperty(PROPERTY_SITE_MARKER_SITE_PROJECT_NAME)
//        layout.projectName.text = projectName
//        val deployedAt = feature.getStringProperty(PROPERTY_SITE_MARKER_SITE_CREATED_AT)
//        layout.deployedAt.text = deployedAt
//        var latLng = ""
//        context?.let { context ->
//            latLng = "${stream?.latitude.latitudeCoordinates(context)}, ${stream?.longitude.longitudeCoordinates(context)}"
//        }
//        layout.latLngTextView.text = latLng
//        val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//        layout.measure(measureSpec, measureSpec)
//        val measuredWidth = layout.measuredWidth
//        layout.arrowPosition = (measuredWidth / 2 - 5).toFloat()
//        val bitmap = SymbolGenerator.generate(layout)
//        windowInfoImages[id] = bitmap
//
//        setWindowInfoImageGenResults(windowInfoImages)
//
//        firebaseCrashlytics.setCustomKey(
//            CrashlyticsKey.WindowInfoDeployment.key,
//            "Site: $title, Project: $projectName"
//        )
//    }

//    private fun setSiteDetail(feature: Feature) {
//        val windowInfoImages = hashMapOf<String, Bitmap>()
//        val inflater = LayoutInflater.from(context)
//
//        val bubbleLayout =
//            inflater.inflate(R.layout.layout_map_window_info, null) as BubbleLayout
//        val id = feature.getStringProperty(PROPERTY_SITE_MARKER_ID) ?: ""
//        val title = feature.getStringProperty(PROPERTY_SITE_MARKER_SITE_NAME)
//        bubbleLayout.infoWindowTitle.text = title
//        val projectName = feature.getStringProperty(PROPERTY_SITE_MARKER_SITE_PROJECT_NAME)
//        bubbleLayout.infoWindowDescription.text = projectName
//        val createdAt = feature.getStringProperty(PROPERTY_SITE_MARKER_SITE_CREATED_AT)
//        bubbleLayout.createdAtValue.text = createdAt
//        var latLng = ""
//        val lat = feature.getStringProperty(PROPERTY_SITE_MARKER_SITE_LATITUDE) ?: "0.0"
//        val lng = feature.getStringProperty(PROPERTY_SITE_MARKER_SITE_LONGITUDE) ?: "0.0"
//        context?.let { context ->
//            latLng = "${lat.toDouble().latitudeCoordinates(context)}, ${lng.toDouble().longitudeCoordinates(context)}"
//        }
//        bubbleLayout.latLngValue.text = latLng
//        val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//        bubbleLayout.measure(measureSpec, measureSpec)
//        val measuredWidth = bubbleLayout.measuredWidth
//        bubbleLayout.arrowPosition = (measuredWidth / 2 - 5).toFloat()
//        val bitmap = SymbolGenerator.generate(bubbleLayout)
//        windowInfoImages[id] = bitmap
//
//        setWindowInfoImageGenResults(windowInfoImages)
//        firebaseCrashlytics.setCustomKey(
//            CrashlyticsKey.WindowInfoSite.key,
//            "Site: $title, Project: $projectName"
//        )
//    }

//    private fun setupImages(style: Style) {
//        val drawablePinSite =
//            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map_grey, null)
//        val mBitmapPinSite = BitmapUtils.getBitmapFromDrawable(drawablePinSite)
//        if (mBitmapPinSite != null) {
//            style.addImage(SITE_MARKER, mBitmapPinSite)
//        }
//
//        val drawablePinMapGreen =
//            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map, null)
//        val mBitmapPinMapGreen = BitmapUtils.getBitmapFromDrawable(drawablePinMapGreen)
//        if (mBitmapPinMapGreen != null) {
//            style.addImage(Pin.PIN_GREEN, mBitmapPinMapGreen)
//        }
//
//        val drawablePinMapGrey =
//            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map_grey, null)
//        val mBitmapPinMapGrey = BitmapUtils.getBitmapFromDrawable(drawablePinMapGrey)
//        if (mBitmapPinMapGrey != null) {
//            style.addImage(Pin.PIN_GREY, mBitmapPinMapGrey)
//        }
//    }

//    private fun setupMarkerLayers(style: Style) {
//
//        val line = LineLayer("line-layer", SOURCE_LINE).withProperties(
//            lineCap(LINE_CAP_ROUND),
//            lineJoin(LINE_JOIN_ROUND),
//            lineWidth(5f),
//            lineColor(get("color"))
//        )
//
//        style.addLayer(line)
//
//        val unclusteredSiteLayer =
//            SymbolLayer(MARKER_SITE_ID, SOURCE_DEPLOYMENT).withProperties(
//                iconImage("{$PROPERTY_SITE_MARKER_IMAGE}"),
//                iconSize(0.8f),
//                iconAllowOverlap(true)
//            )
//
//        val unclusteredDeploymentLayer =
//            SymbolLayer(MARKER_DEPLOYMENT_ID, SOURCE_DEPLOYMENT).withProperties(
//                iconImage("{$PROPERTY_DEPLOYMENT_MARKER_IMAGE}"),
//                iconSize(
//                    match(
//                        toString(
//                            get(
//                                PROPERTY_DEPLOYMENT_SELECTED
//                            )
//                        ),
//                        literal(0.8f), stop("true", 1.0f)
//                    )
//                ),
//                iconAllowOverlap(true)
//            )
//
//        style.addLayer(unclusteredSiteLayer)
//        style.addLayer(unclusteredDeploymentLayer)
//
//        val layers = arrayOf(
//            intArrayOf(0, Color.parseColor("#98A0A9")),
//            intArrayOf(1, Color.parseColor("#2AA841"))
//        )
//
//        layers.forEachIndexed { i, ly ->
//            val deploymentSymbolLayer = CircleLayer("$DEPLOYMENT_CLUSTER-$i", SOURCE_DEPLOYMENT)
//            val hasDeploymentAtLeastOne = toNumber(get(PROPERTY_CLUSTER_TYPE))
//            val pointCount = toNumber(get(POINT_COUNT))
//            deploymentSymbolLayer.setProperties(circleColor(ly[1]), circleRadius(16f))
//            deploymentSymbolLayer.setFilter(
//                if (i == 0) {
//                    all(
//                        gte(hasDeploymentAtLeastOne, literal(ly[0])),
//                        gte(pointCount, literal(1))
//                    )
//                } else {
//                    all(
//                        gte(hasDeploymentAtLeastOne, literal(ly[0])),
//                        gt(hasDeploymentAtLeastOne, literal(layers[i - 1][0]))
//                    )
//                }
//            )
//
//            style.addLayer(deploymentSymbolLayer)
//        }
//
//        val deploymentCount = SymbolLayer(DEPLOYMENT_COUNT, SOURCE_DEPLOYMENT)
//        deploymentCount.setProperties(
//            textField(
//                format(
//                    formatEntry(
//                        toString(get(POINT_COUNT)),
//                        FormatOption.formatFontScale(1.5)
//                    )
//                )
//            ),
//            textSize(12f),
//            textColor(Color.WHITE),
//            textIgnorePlacement(true),
//            textOffset(arrayOf(0f, -0.2f)),
//            textAllowOverlap(true)
//        )
//
//        style.addLayer(deploymentCount)
//    }

//    private fun handleClickIcon(screenPoint: PointF): Boolean {
////        val deploymentFeatures = mapboxMap?.queryRenderedFeatures(screenPoint, MARKER_DEPLOYMENT_ID)
////        val siteFeatures = mapboxMap?.queryRenderedFeatures(screenPoint, MARKER_SITE_ID)
////        val deploymentClusterFeatures =
////            mapboxMap?.queryRenderedFeatures(screenPoint, "$DEPLOYMENT_CLUSTER-0")
//        if (deploymentFeatures != null && deploymentFeatures.isNotEmpty()) {
//            val selectedFeature = deploymentFeatures[0]
//            val features = this.mapFeatures!!.features()!!
//            features.forEachIndexed { index, feature ->
//                if (selectedFeature.getProperty(PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID) == feature.getProperty(
//                        PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID
//                    )
//                ) {
//                    val markerId = selectedFeature.getProperty(
//                        PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID
//                    ).asString
//                    val deploymentId = markerId.split(".").last()
//                    val deployment = mainViewModel.getDeploymentById(deploymentId.toInt())
//                    val site = mainViewModel.getStreamById(deployment?.stream?.id ?: -1)
//                    gettingTracksAndMoveToPin(site, markerId)
//                    analytics?.trackClickPinEvent()
//
//                    features[index]?.let {
//                        setDeploymentDetail(it)
//                        setFeatureSelectState(it, true)
//                    }
//                } else {
//                    features[index]?.let { setFeatureSelectState(it, false) }
//                }
//            }
//            return true
//        } else {
//            (activity as MainActivityListener).hideBottomSheet()
//            hideTrackOnMap()
//        }
//
//        if (siteFeatures != null && siteFeatures.isNotEmpty()) {
//
//            val selectedFeature = siteFeatures[0]
//            val features = this.mapFeatures!!.features()!!
//            features.forEachIndexed { index, feature ->
//                val markerId = selectedFeature.getProperty(PROPERTY_SITE_MARKER_ID)
//                if (markerId == feature.getProperty(PROPERTY_SITE_MARKER_ID)) {
//                    val site = mainViewModel.getStreamById(
//                        selectedFeature.getProperty(PROPERTY_SITE_MARKER_SITE_ID).asInt
//                    )
//                    gettingTracksAndMoveToPin(site, markerId.asString)
//                    features[index]?.let {
//                        setSiteDetail(it)
//                        setFeatureSelectState(it, true)
//                    }
//                    analytics?.trackClickPinEvent()
//                } else {
//                    features[index]?.let { setFeatureSelectState(it, false) }
//                }
//            }
//            return true
//        } else {
//            hideTrackOnMap()
//        }
//
//        if (deploymentClusterFeatures != null && deploymentClusterFeatures.isNotEmpty()) {
//            val pinCount =
//                if (deploymentClusterFeatures[0].getProperty(POINT_COUNT) != null) deploymentClusterFeatures[0].getProperty(
//                    POINT_COUNT
//                ).asInt else 0
//            if (pinCount > 0) {
//                val clusterLeavesFeatureCollection =
//                    mapSource?.getClusterLeaves(deploymentClusterFeatures[0], 8000, 0)
//                if (clusterLeavesFeatureCollection != null) {
//                    moveCameraToLeavesBounds(clusterLeavesFeatureCollection)
//                }
//            }
//        }
//        clearFeatureSelected()
//        return false
//    }

//    private fun handleClickCallout(feature: Feature): Boolean {
//        val deploymentId = feature.getStringProperty(PROPERTY_DEPLOYMENT_MARKER_DEPLOYMENT_ID)
//        val siteName = feature.getStringProperty(PROPERTY_DEPLOYMENT_MARKER_TITLE)
//        val projectName = feature.getStringProperty(PROPERTY_SITE_MARKER_SITE_PROJECT_NAME)
//
//        if (deploymentId != null) {
//            context?.let {
//                firebaseCrashlytics.setCustomKey(
//                    CrashlyticsKey.OnClickSeeDetail.key,
//                    "Site: $siteName, Project: $projectName"
//                )
//                DeploymentDetailActivity.startActivity(it, deploymentId.toInt())
//                analytics?.trackSeeDetailEvent()
//            }
//        } else {
//            setFeatureSelectState(feature, false)
//        }
//        return true
//    }

    fun gettingTracksAndMoveToPin(site: Stream?, markerId: String) {
        currentMarkId = markerId
        site?.let { obj ->
//            showTrackOnMap(obj.id, obj.latitude, obj.longitude, markerId)
            if (site.serverId != null) {
                mainViewModel.getStreamAssets(site)
                setTrackObserver(obj, markerId)
            }
        }
    }

//    private fun moveCameraToLeavesBounds(featureCollectionToInspect: FeatureCollection) {
//        val latLngList: ArrayList<LatLng> = ArrayList()
//        if (featureCollectionToInspect.features() != null) {
//            for (singleClusterFeature in featureCollectionToInspect.features()!!) {
//                val clusterPoint = singleClusterFeature.geometry() as Point?
//                if (clusterPoint != null) {
//                    latLngList.add(LatLng(clusterPoint.latitude(), clusterPoint.longitude()))
//                }
//            }
//            if (latLngList.size > 1) {
//                moveCameraWithLatLngList(latLngList)
//            }
//        }
//    }

//    private fun moveCameraWithLatLngList(latLngList: List<LatLng>) {
//        val latLngBounds = LatLngBounds.Builder()
//            .includes(latLngList)
//            .build()
//        mapboxMap?.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 230), 1300)
//    }

    private fun updateUnsyncedCount(number: Int) {
        if (number == 0) {
            unSyncedDpNumber.text = ""
            unSyncedDpNumber.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circledp)
        } else {
            unSyncedDpNumber.text = number.toString()
            unSyncedDpNumber.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.circle_unsynced)
        }
    }

    private fun combinedData() {
//        handleMarker(deploymentMarkers + streamMarkers)
        mClusterManager.clearItems()
        setMarker(deploymentMarkers + streamMarkers)

//        val state = listener?.getBottomSheetState() ?: 0
//
//        if (deploymentMarkers.isNotEmpty() && state != BottomSheetBehavior.STATE_EXPANDED) {
//            val lastReport = deploymentMarkers.sortedByDescending { it.updatedAt }.first()
//            mapboxMap?.let {
//                it.moveCamera(
//                    CameraUpdateFactory.newLatLngZoom(
//                        LatLng(
//                            lastReport.latitude,
//                            lastReport.longitude
//                        ),
//                        it.cameraPosition.zoom
//                    )
//                )
//            }
//        }
//
        val currentLocation = currentUserLocation
        if (currentLocation != null) {
            adapterOfSearchSite = getListSite(
                currentLocation,
                streams
            )
            context?.let { currentLocation.saveLastLocation(it) }
        } else {
            adapterOfSearchSite = getListSiteWithOutCurrentLocation(
                streams
            )
        }
        siteAdapter.items = adapterOfSearchSite ?: listOf()

        if (adapterOfSearchSite.isNullOrEmpty()) {
            showLabel(false)
        } else {
            hideLabel()
        }
    }

//    private fun getFurthestSiteFromCurrentLocation(
//        currentLatLng: LatLng,
//        sites: List<Stream>
//    ): Stream? {
//        return sites.maxByOrNull {
//            currentLatLng.distanceTo(LatLng(it.latitude, it.longitude))
//        }
//    }

    private fun setObserver() {
        mainViewModel.getUnsyncedWorks()
            .observe(viewLifecycleOwner, getUnsyncedDeploymentsObserver)
        mainViewModel.getProjectsFromRemote()
            .observe(viewLifecycleOwner, getProjectsFromRemoteObserver)
        mainViewModel.getDeploymentMarkers()
            .observe(viewLifecycleOwner, getDeploymentMarkerObserver)
        mainViewModel.getStreamMarkers().observe(viewLifecycleOwner, getStreamMarkerObserver)
        mainViewModel.getStreams().observe(viewLifecycleOwner, getStreamObserver)
    }

    private fun setTrackObserver(site: Stream, markerId: String) {
        mainViewModel.getTrackingFromRemote().observe(
            viewLifecycleOwner,
            Observer {
                when (it.status) {
                    Status.LOADING -> {
                    }

                    Status.SUCCESS -> {
//                        showTrackOnMap(
//                            site.id,
//                            site.latitude,
//                            site.longitude,
//                            markerId
//                        )
                    }

                    Status.ERROR -> {
                        showToast(it.message ?: getString(R.string.error_has_occurred))
                    }
                }
            }
        )
    }

    private fun fetchJobSyncing() {
        context ?: return
        downloadStreamsWorkInfoLiveData = DownloadStreamsWorker.workInfos(requireContext())
        downloadStreamsWorkInfoLiveData.observeForever(downloadStreamsWorkInfoObserve)
        deploymentWorkInfoLiveData = DeploymentSyncWorker.workInfos(requireContext())
        deploymentWorkInfoLiveData.observeForever(deploymentWorkInfoObserve)
    }

    private fun updateSyncInfo(syncInfo: SyncInfo? = null, isSites: Boolean) {
        val status = syncInfo
            ?: if (context.isNetworkAvailable()) SyncInfo.Starting else SyncInfo.WaitingNetwork
        if (this.lastSyncingInfo == SyncInfo.Uploaded && status == SyncInfo.Uploaded) return
        this.lastSyncingInfo = status
        setSnackbar(status, isSites)
    }

    private fun setSnackbar(status: SyncInfo, isSites: Boolean) {
        val deploymentUnsentCount = mainViewModel.getDeploymentUnsentCount()
        when (status) {
            SyncInfo.Starting, SyncInfo.Uploading -> {
                val msg = if (isSites) {
                    getString(R.string.sites_downloading)
                } else {
                    if (deploymentUnsentCount > 1) {
                        getString(
                            R.string.format_deploys_uploading,
                            deploymentUnsentCount.toString()
                        )
                    } else {
                        getString(R.string.format_deploy_uploading)
                    }
                }
                statusView.onShow(msg)
            }

            SyncInfo.Uploaded -> {
                val msg = if (isSites) {
                    getString(R.string.sites_synced)
                } else {
                    getString(R.string.format_deploys_uploaded)
                }
                statusView.onShowWithDelayed(msg)
            }
            // else also waiting network
            else -> {
                if (!isSites) statusView.onShowWithDelayed(getString(R.string.format_deploy_waiting_network))
            }
        }
    }

//    private fun handleMarker(
//        mapMarker: List<MapMarker>
//    ) {
//        val deploymentFeatures = this.mapFeatures?.features()
//        val deploymentSelecting = deploymentFeatures?.firstOrNull { feature ->
//            feature.getBooleanProperty(PROPERTY_DEPLOYMENT_SELECTED) ?: false
//        }
//
//        // Create point
//        val mapMarkerPointFeatures = mapMarker.map {
//            // check is this deployment is selecting (to set bigger pin)
//            when (it) {
//                is MapMarker.DeploymentMarker -> {
//                    val deploymentId =
//                        deploymentSelecting?.getProperty(PROPERTY_DEPLOYMENT_MARKER_DEPLOYMENT_ID)
//                    val isSelecting =
//                        if (deploymentSelecting == null || deploymentId == null) {
//                            false
//                        } else {
//                            it.id.toString() == deploymentId.asString
//                        }
//                    val properties = mapOf(
//                        Pair(PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID, "${it.locationName}.${it.id}"),
//                        Pair(PROPERTY_WINDOW_INFO_ID, "${it.locationName}.${it.id}"),
//                        Pair(PROPERTY_DEPLOYMENT_MARKER_IMAGE, it.pin),
//                        Pair(PROPERTY_DEPLOYMENT_MARKER_TITLE, it.locationName),
//                        Pair(PROPERTY_DEPLOYMENT_MARKER_DEPLOYMENT_ID, it.id.toString()),
//                        Pair(PROPERTY_DEPLOYMENT_MARKER_DEVICE, it.device),
//                        Pair(PROPERTY_SITE_MARKER_SITE_PROJECT_NAME, it.projectName ?: ""),
//                        Pair(
//                            PROPERTY_SITE_MARKER_SITE_CREATED_AT,
//                            context?.let { context -> it.deploymentAt.toTimeAgo(context) } ?: ""
//                        ),
//                        Pair(PROPERTY_DEPLOYMENT_SELECTED, isSelecting.toString())
//                    )
//                    Feature.fromGeometry(
//                        Point.fromLngLat(it.longitude, it.latitude), properties.toJsonObject()
//                    )
//                }
//                is MapMarker.SiteMarker -> {
//                    val properties = mapOf(
//                        Pair(PROPERTY_SITE_MARKER_IMAGE, it.pin),
//                        Pair(PROPERTY_WINDOW_INFO_ID, "${it.name}.${it.id}"),
//                        Pair(PROPERTY_SITE_MARKER_SITE_ID, it.id.toString()),
//                        Pair(PROPERTY_SITE_MARKER_ID, "${it.name}.${it.id}"),
//                        Pair(PROPERTY_SITE_MARKER_SITE_NAME, it.name),
//                        Pair(PROPERTY_SITE_MARKER_SITE_LATITUDE, "${it.latitude}"),
//                        Pair(PROPERTY_SITE_MARKER_SITE_LONGITUDE, "${it.longitude}"),
//                        Pair(PROPERTY_SITE_MARKER_SITE_PROJECT_NAME, it.projectName ?: ""),
//                        Pair(
//                            PROPERTY_SITE_MARKER_SITE_CREATED_AT,
//                            context?.let { context -> it.createdAt.toTimeAgo(context) } ?: ""
//                        )
//                    )
//
//                    Feature.fromGeometry(
//                        Point.fromLngLat(it.longitude, it.latitude), properties.toJsonObject()
//                    )
//                }
//            }
//        }
//        this.mapFeatures = FeatureCollection.fromFeatures(mapMarkerPointFeatures)
//        refreshSource()
//    }

//    private fun setWindowInfoImageGenResults(windowInfoImages: HashMap<String, Bitmap>) {
//        mapboxMap?.style?.addImages(windowInfoImages)
//    }

//    private fun setupWindowInfo(it: Style) {
//        it.addLayer(
//            SymbolLayer(WINDOW_MARKER_ID, SOURCE_DEPLOYMENT).apply {
//                withProperties(
//                    iconImage("{$PROPERTY_WINDOW_INFO_ID}"),
//                    iconAnchor(ICON_ANCHOR_BOTTOM),
//                    iconOffset(arrayOf(-2f, -20f)),
//                    iconAllowOverlap(true)
//                )
//                withFilter(eq(get(PROPERTY_DEPLOYMENT_SELECTED), literal(true)))
//            }
//        )
//    }

//    private fun refreshSource() {
//        if (mapSource != null && mapFeatures != null) {
//            mapSource!!.setGeoJson(mapFeatures)
//        }
//    }

//    fun moveCamera(userLoc: LatLng, targetLoc: LatLng? = null, zoom: Double) {
//        mapboxMap?.moveCamera(MapboxCameraUtils.calculateLatLngForZoom(userLoc, targetLoc, zoom))
//    }

//    private fun checkThenAccquireLocation(style: Style) {
//        locationPermissions?.check { isAllowed: Boolean ->
//            if (isAllowed) {
//                enableLocationComponent(style)
//            }
//        }
//    }

//    private fun enableLocationComponent(style: Style) {
//        val customLocationComponentOptions = context?.let {
//            LocationComponentOptions.builder(it)
//                .trackingGesturesManagement(true)
//                .accuracyColor(ContextCompat.getColor(it, R.color.colorPrimary))
//                .build()
//        }
//
//        val locationComponentActivationOptions =
//            context?.let {
//                LocationComponentActivationOptions.builder(it, style)
//                    .locationComponentOptions(customLocationComponentOptions)
//                    .build()
//            }
//
//        mapboxMap?.let { it ->
//            it.locationComponent.apply {
//                if (locationComponentActivationOptions != null) {
//                    activateLocationComponent(locationComponentActivationOptions)
//                    isLocationComponentEnabled = true
//                    cameraMode = CameraMode.TRACKING
//                    renderMode = RenderMode.COMPASS
//                }
//            }
//        }
//
//        initLocationEngine()
//    }

//    private fun initLocationEngine() {
//        locationEngine = context?.let { LocationEngineProvider.getBestLocationEngine(it) }
//        val request =
//            LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
//                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
//                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()
//
//        locationEngine?.requestLocationUpdates(
//            request,
//            mapboxLocationChangeCallback,
//            Looper.getMainLooper()
//        )
//
//        locationEngine?.getLastLocation(mapboxLocationChangeCallback)
//    }

//    private fun moveCameraOnStartWithProject() {
//        mapboxMap?.locationComponent?.lastKnownLocation?.let { curLoc ->
//            val currentLatLng = LatLng(curLoc.latitude, curLoc.longitude)
//            val projectName = listener?.getProjectName()
//            val locations = this.streams.filter { it.project?.name == projectName }
//            val furthestSite = getFurthestSiteFromCurrentLocation(
//                currentLatLng,
//                if (projectName != getString(R.string.none)) locations else this.streams
//            )
//            furthestSite?.let {
//                moveCamera(
//                    currentLatLng,
//                    LatLng(furthestSite.latitude, furthestSite.longitude),
//                    DefaultSetupMap.DEFAULT_ZOOM
//                )
//            }
//            context?.let { currentLatLng.saveLastLocation(it) }
//        }
//    }

//    private fun moveCameraToCurrentLocation() {
//        mapboxMap?.locationComponent?.lastKnownLocation?.let { curLoc ->
//            val currentLatLng = LatLng(curLoc.latitude, curLoc.longitude)
//            moveCamera(
//                currentLatLng,
//                null,
//                mapboxMap?.cameraPosition?.zoom ?: DefaultSetupMap.DEFAULT_ZOOM
//            )
//        }
//    }

//    fun moveToDeploymentMarker(lat: Double, lng: Double) {
//        mapboxMap?.let {
//            it.moveCamera(
//                CameraUpdateFactory.newLatLngZoom(
//                    LatLng(lat, lng),
//                    mapboxMap?.cameraPosition?.zoom ?: DefaultSetupMap.DEFAULT_ZOOM
//                )
//            )
//        }
//    }

//    fun showTrackOnMap(id: Int, lat: Double, lng: Double, markerLocationId: String) {
//        // remove the previous one
//        hideTrackOnMap()
//        val tracks = mainViewModel.getTrackingFileBySiteId(id)
//        try {
//            if (tracks.isNotEmpty()) {
//                // get all track first
//                if (currentMarkId == markerLocationId) {
//                    val tempTrack = arrayListOf<Feature>()
//                    tracks.forEach { track ->
//                        val json = File(track.localPath).readText()
//                        val featureCollection = FeatureCollection.fromJson(json)
//                        val feature = featureCollection.features()?.get(0)
//                        feature?.let {
//                            tempTrack.add(it)
//                        }
//                        // track always has 1 item so using get(0) is okay - also it can only be LineString
//                        val lineString = feature?.geometry() as LineString
//                        queue.add(lineString.coordinates().toList())
//                    }
//                    lineSource?.setGeoJson(FeatureCollection.fromFeatures(tempTrack))
//
//                    // move camera to pin
//                    moveToDeploymentMarker(lat, lng)
//                }
//            } else {
//                moveToDeploymentMarker(lat, lng)
//            }
//        } catch (e: JsonSyntaxException) {
//            moveToDeploymentMarker(lat, lng)
//        }
//    }

//    private fun hideTrackOnMap() {
//        // reset source
//        lineSource?.setGeoJson(FeatureCollection.fromFeatures(listOf()))
//        routeCoordinateList = listOf()
//        routeIndex = 0
//        markerLinePointList.clear()
//        queuePivot = 0
//        queue.clear()
//        queueColor.clear()
//        currentAnimator?.end()
//        currentAnimator = null
//    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun showButtonOnMap() {
        buttonOnMapGroup.visibility = View.VISIBLE
    }

    fun hideButtonOnMap() {
        buttonOnMapGroup.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
//        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
//        mapView.onResume()
        analytics?.trackScreen(Screen.MAP)
    }

    override fun onPause() {
        super.onPause()
//        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
//        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
//        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::mainViewModel.isInitialized) {
            mainViewModel.onDestroy()
            mainViewModel.getProjectsFromRemote().removeObserver(getProjectsFromRemoteObserver)
            mainViewModel.getDeploymentMarkers().removeObserver(getDeploymentMarkerObserver)
            mainViewModel.getStreamMarkers().removeObserver(getStreamMarkerObserver)
            mainViewModel.getStreams().removeObserver(getStreamObserver)
            mainViewModel.getUnsyncedWorks().removeObserver(getUnsyncedDeploymentsObserver)
        }
        if (::deploymentWorkInfoLiveData.isInitialized) {
            deploymentWorkInfoLiveData.removeObserver(deploymentWorkInfoObserve)
        }
        if (::downloadStreamsWorkInfoLiveData.isInitialized) {
            downloadStreamsWorkInfoLiveData.removeObserver(downloadStreamsWorkInfoObserve)
        }
//        if (::mapView.isInitialized) {
//            mapView.onDestroy()
//        }

//        locationEngine?.removeLocationUpdates(mapboxLocationChangeCallback)
        currentAnimator?.cancel()
    }

    companion object {
        const val tag = "MapFragment"
        const val SITE_MARKER = "SITE_MARKER"

        private const val SOURCE_DEPLOYMENT = "source.deployment"
        private const val MARKER_DEPLOYMENT_ID = "marker.deployment"
        private const val MARKER_SITE_ID = "marker.site"

        private const val SOURCE_LINE = "source.line"

        private const val PROPERTY_DEPLOYMENT_SELECTED = "deployment.selected"
        private const val PROPERTY_DEPLOYMENT_MARKER_DEVICE = "deployment.device"
        private const val PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID = "deployment.location"
        private const val PROPERTY_DEPLOYMENT_MARKER_TITLE = "deployment.title"
        private const val PROPERTY_DEPLOYMENT_MARKER_DEPLOYMENT_ID = "deployment.deployment"
        private const val PROPERTY_DEPLOYMENT_MARKER_IMAGE = "deployment.marker.image"
        private const val WINDOW_MARKER_ID = "info.marker"
        private const val PROPERTY_WINDOW_INFO_ID = "window.info.id"

        private const val PROPERTY_SITE_MARKER_IMAGE = "site.marker.image"
        private const val PROPERTY_SITE_MARKER_ID = "site.id"
        private const val PROPERTY_SITE_MARKER_SITE_ID = "site.stream.id"
        private const val PROPERTY_SITE_MARKER_SITE_NAME = "site.stream.name"
        private const val PROPERTY_SITE_MARKER_SITE_LATITUDE = "site.stream.latitude"
        private const val PROPERTY_SITE_MARKER_SITE_LONGITUDE = "site.stream.longitude"
        private const val PROPERTY_SITE_MARKER_SITE_PROJECT_NAME = "site.stream.project.name"
        private const val PROPERTY_SITE_MARKER_SITE_CREATED_AT = "site.stream.created.at"

        private const val PROPERTY_CLUSTER_TYPE = "cluster.type"

        private const val DEPLOYMENT_CLUSTER = "deployment.cluster"
        private const val POINT_COUNT = "point_count"
        private const val DEPLOYMENT_COUNT = "deployment.count"
        private const val WITHIN_TIME = (60 * 3) // 3 hr
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        private const val DURATION = 700

        const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

        fun newInstance(): MapFragment {
            return MapFragment()
        }
    }

    override fun invoke(stream: Stream, isNew: Boolean) {
        view?.hideKeyboard()
        showSearchBar(false)

        val latLng = LatLng(stream.latitude, stream.longitude)
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng))

        mClusterManager.markerCollection.markers.forEach {
            if (it.snippet!!.contains(stream.name)) {
                it.showInfoWindow()
            }
        }
    }

    override fun onClicked(project: Project) {
        projectRecyclerView.visibility = View.GONE
        projectSwipeRefreshView.visibility = View.GONE

        context?.let { context ->
            Preferences.getInstance(context).putInt(Preferences.SELECTED_PROJECT, project.id)
            // reload site to get sites from selected project
            mainViewModel.retrieveLocations()
        }

        projectNameTextView.text = project.name
        mainViewModel.combinedData()
//        val latLngBounds =
//            project.streams?.map { LatLng(it.latitude, it.longitude) } ?: listOf()
//        if (latLngBounds.isNotEmpty()) {
//            if (latLngBounds.size > 1) {
//                moveCameraWithLatLngList(latLngBounds)
//            } else {
//                moveCamera(
//                    LatLng(latLngBounds[0].latitude, latLngBounds[0].longitude),
//                    null,
//                    DefaultSetupMap.DEFAULT_ZOOM
//                )
//            }
//        } else {
//            currentUserLocation?.let { current ->
//                moveCamera(
//                    LatLng(
//                        current.latitude,
//                        current.longitude
//                    ),
//                    null, DefaultSetupMap.DEFAULT_ZOOM
//                )
//            }
//        }

        if (siteRecyclerView.visibility == View.VISIBLE) {
            searchLayout.visibility = View.VISIBLE
        } else {
            searchButton.visibility = View.VISIBLE
            trackingLayout.visibility = View.VISIBLE
            showButtonOnMap()
            listener?.showBottomAppBar()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {

        // Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            return
        }

        // Otherwise, request permission
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onLockImageClicked() {
        Toast.makeText(context, R.string.not_have_permission, Toast.LENGTH_LONG).show()
    }
}
