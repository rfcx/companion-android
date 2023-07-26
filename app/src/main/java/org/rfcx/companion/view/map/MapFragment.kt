package org.rfcx.companion.view.map

import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.os.Build
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.maps.android.clustering.Cluster
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
import java.io.File
import java.util.*

class MapFragment :
    Fragment(),
    ProjectListener,
    OnMapReadyCallback,
    (Stream, Boolean) -> Unit,
    ClusterManager.OnClusterClickListener<MarkerItem>,
    ClusterManager.OnClusterItemClickListener<MarkerItem>,
    ClusterManager.OnClusterItemInfoWindowClickListener<MarkerItem> {

    // Google map
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mClusterManager: ClusterManager<MarkerItem>
    private lateinit var mainViewModel: MainViewModel

    // database manager
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val trackingDb by lazy { TrackingDb(realm) }

    // data
    private var streams = listOf<Stream>()
    private var projects = listOf<Project>()
    private var lastSyncingInfo: SyncInfo? = null

    private var deploymentMarkers = listOf<MapMarker.DeploymentMarker>()
    private var unsyncedDeploymentCount = 0
    private var lastSelectedId = -1
    private var streamMarkers = listOf<MapMarker>()

    private lateinit var deploymentWorkInfoLiveData: LiveData<List<WorkInfo>>
    private lateinit var downloadStreamsWorkInfoLiveData: LiveData<List<WorkInfo>>

    private val locationPermissions by lazy { activity?.let { LocationPermissions(it) } }
    private val notificationPermissions by lazy { activity?.let { NotificationPermissions(it) } }
    private var listener: MainActivityListener? = null

    private var currentUserLocation: Location? = null

    private val analytics by lazy { context?.let { Analytics(it) } }
    private val firebaseCrashlytics by lazy { Crashlytics() }

    private val handler: Handler = Handler()

    private var currentAnimator: Animator? = null
    private var polyline: Polyline? = null
    private var currentMarkId = ""
    private var screen = ""
    private var lastZoom = DefaultSetupMap.DEFAULT_ZOOM

    private val siteAdapter by lazy { SiteAdapter(this) }
    private var adapterOfSearchSite: List<SiteWithLastDeploymentItem>? = null
    private val locationGroupAdapter by lazy { ProjectAdapter(this) }

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

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(), R.raw.style_json
                )
            )
        } catch (_: Resources.NotFoundException) {
        }
        mainViewModel.retrieveLocations()
        mainViewModel.fetchProjects()
        setUpClusterer()
        setupSearch()
        setObserver()
        showSearchBar(false)

        if (locationPermissions?.allowed() == false) {
            locationPermissions?.check { /* do nothing */ }
        } else {
            enableMyLocation()
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                map.moveCamera(
                    CameraUpdateFactory.newLatLng(
                        LatLng(
                            location?.latitude ?: 0.0,
                            location?.longitude ?: 0.0
                        )
                    )
                )
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
        // can re-cluster when zooming in and out.
        map.setOnCameraIdleListener {
            mClusterManager.onCameraIdle()
        }

        map.setOnMarkerClickListener(mClusterManager)
        map.setInfoWindowAdapter(mClusterManager.markerManager)
        map.setOnInfoWindowClickListener(mClusterManager)
        mClusterManager.setOnClusterClickListener(this)
        mClusterManager.setOnClusterItemClickListener(this)
        mClusterManager.setOnClusterItemInfoWindowClickListener(this)

        map.setOnMapClickListener {
            lastSelectedId = -1
            polyline?.remove()
        }

        combinedData()
    }

    override fun onClusterClick(cluster: Cluster<MarkerItem>?): Boolean {
        val builder = LatLngBounds.builder()
        val markers: Collection<MarkerItem> = cluster!!.items

        for (item in markers) {
            val position = item.position
            builder.include(position)
        }

        val bounds = builder.build()

        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } catch (error: Exception) {
            return true
        }
        return true
    }

    override fun onClusterItemClick(item: MarkerItem?): Boolean {
        polyline?.remove()

        val isDeployment = item?.let { isDeployment(it.snippet) } ?: false
        if (isDeployment) {
            val data = Gson().fromJson(item!!.snippet, InfoWindowMarker::class.java)
            val deployment = mainViewModel.getDeploymentById(data.id)
            val site = mainViewModel.getStreamById(deployment?.stream?.id ?: -1)
            gettingTracksAndMoveToPin(site, "${data.locationName}.${data.id}")
        }
        return false
    }

    override fun onClusterItemInfoWindowClick(item: MarkerItem?) {
        if (item?.snippet == null) return
        val isDeployment = isDeployment(item.snippet)

        if (isDeployment) {
            val data = Gson().fromJson(item.snippet, InfoWindowMarker::class.java)
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
        val item = MarkerItem(
            data.latitude,
            data.longitude,
            data.name,
            Gson().toJson(data.toInfoWindowMarker())
        )
        mClusterManager.addItem(item)
        mClusterManager.cluster()

        // Move Camera
        map.moveCamera(CameraUpdateFactory.newLatLng(latlng))
        map.animateCamera(CameraUpdateFactory.zoomTo(13.0f))
    }

    private fun setMarker(data: MapMarker.DeploymentMarker) {
        // Add Marker
        val latlng = LatLng(data.latitude, data.longitude)
        val item = MarkerItem(
            data.latitude,
            data.longitude,
            data.locationName,
            Gson().toJson(data.toInfoWindowMarker())
        )
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
        notificationPermissions?.handleRequestResult(requestCode, grantResults)
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setViewModel()
        fetchJobSyncing()
        hideLabel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (notificationPermissions?.notificationAllowed() == false) {
                notificationPermissions?.check { /* do nothing */ }
            }
        }

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
            lastZoom = map.cameraPosition.zoom
            map.moveCamera(CameraUpdateFactory.zoomTo(21.0F))
            setSearchView()
            searchLayout.setBackgroundResource(R.color.backgroundColorSite)
        } else {
            map.moveCamera(CameraUpdateFactory.zoomTo(lastZoom))
            searchLayoutSearchEditText.text = null
            searchLayout.setBackgroundResource(R.color.transparent)

            hideLabel()
            siteRecyclerView.visibility = View.GONE
            listener?.showBottomAppBar()
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
                trackingTextView.text = "${LocationTrackingManager.getDistance(trackingDb).setFormatLabel()}  ${LocationTrackingManager.getOnDutyTimeMinute(it)} min"
            }
            handler.postDelayed(this, 20 * 1000L)
        }
    }

    fun gettingTracksAndMoveToPin(site: Stream?, markerId: String) {
        currentMarkId = markerId
        site?.let { obj ->
            lastSelectedId = site.id
            showTrackOnMap(obj.id, obj.latitude, obj.longitude, markerId)
            if (site.serverId != null) {
                mainViewModel.getStreamAssets(site)
                setTrackObserver(obj, markerId)
            }
        }
    }

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
        mClusterManager.clearItems()
        mClusterManager.cluster()

        setMarker(deploymentMarkers + streamMarkers)

        val state = listener?.getBottomSheetState() ?: 0

        if (deploymentMarkers.isNotEmpty() && state != BottomSheetBehavior.STATE_EXPANDED) {
            val lastReport = deploymentMarkers.sortedByDescending { it.updatedAt }.first()
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        lastReport.latitude,
                        lastReport.longitude
                    ),
                    map.cameraPosition.zoom
                )
            )
        }

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
                        if (lastSelectedId == site.id) {
                            showTrackOnMap(
                                site.id,
                                site.latitude,
                                site.longitude,
                                markerId
                            )
                        }
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

    @SuppressLint("Range")
    fun showTrackOnMap(id: Int, lat: Double, lng: Double, markerLocationId: String) {
        val tracks = mainViewModel.getTrackingFileBySiteId(id)
        try {
            tracks.forEach { track ->
                if (track.siteServerId != null) {
                    val json = File(track.localPath).readText()
                    val f = Gson().fromJson(json, FeatureCollection::class.java)
                    val latLngList = mutableListOf<LatLng>()
                    f.features.forEach {
                        it.geometry.coordinates.forEach { c ->
                            latLngList.add(LatLng(c[1], c[0]))
                        }
                    }
                    if (lastSelectedId == id) {
                        polyline?.remove()
                        polyline = map.addPolyline(
                            PolylineOptions()
                                .clickable(false)
                                .addAll(latLngList)
                                .color(Color.parseColor(f.features[0].properties.color))
                        )
                    }
                }
            }
        } catch (_: JsonSyntaxException) {
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun showButtonOnMap() {
        buttonOnMapGroup.visibility = View.VISIBLE
    }

    fun hideButtonOnMap() {
        buttonOnMapGroup.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.MAP)
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
        currentAnimator?.cancel()
    }

    companion object {
        const val tag = "MapFragment"
        const val SITE_MARKER = "SITE_MARKER"

        private const val WITHIN_TIME = (60 * 3) // 3 hr
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

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
        map.moveCamera(CameraUpdateFactory.zoomTo(20.0F))
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng))

        val clusters = mClusterManager.algorithm.getClusters(21.0F)
        val markerItems = arrayListOf<MarkerItem>()

        clusters.forEach { cluster ->
            cluster.items.forEach { item ->
                markerItems.add(item)
            }
        }

        mClusterManager.markerCollection.markers.forEach {
            if (it.snippet!!.contains(stream.name)) {
                it.showInfoWindow()
                onClusterItemClick(markerItems.first { i -> i.snippet.contains(stream.name) })
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
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
