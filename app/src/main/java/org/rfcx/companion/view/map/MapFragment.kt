package org.rfcx.companion.view.map

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.work.WorkInfo
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.*
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mapbox.pluginscalebar.ScaleBarOptions
import com.mapbox.pluginscalebar.ScaleBarPlugin
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_map.*
import org.rfcx.companion.DeploymentListener
import org.rfcx.companion.MainActivityListener
import org.rfcx.companion.R
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.guardian.toMark
import org.rfcx.companion.entity.response.DeploymentImageResponse
import org.rfcx.companion.entity.response.DeploymentResponse
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.localdb.guardian.DiagnosticDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.repo.Firestore
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.locate.LocationFragment
import org.rfcx.companion.view.profile.locationgroup.LocationGroupActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapFragment : Fragment(), OnMapReadyCallback {

    // map
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private var locationEngine: LocationEngine? = null
    private var mapSource: GeoJsonSource? = null
    private var mapFeatures: FeatureCollection? = null

    // database manager
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val guardianDeploymentDb by lazy { GuardianDeploymentDb(realm) }
    private val locateDb by lazy { LocateDb(realm) }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }
    private val diagnosticDb by lazy { DiagnosticDb(realm) }

    // data
    private var guardianDeployments = listOf<GuardianDeployment>()
    private var edgeDeployments = listOf<EdgeDeployment>()
    private var locations = listOf<Locate>()
    private var locationGroups = listOf<LocationGroups>()
    private var lastSyncingInfo: SyncInfo? = null

    private lateinit var guardianDeployLiveData: LiveData<List<GuardianDeployment>>
    private lateinit var edgeDeployLiveData: LiveData<List<EdgeDeployment>>
    private lateinit var locateLiveData: LiveData<List<Locate>>
    private lateinit var locationGroupLiveData: LiveData<List<LocationGroups>>
    private lateinit var deploymentWorkInfoLiveData: LiveData<List<WorkInfo>>

    private val locationPermissions by lazy { activity?.let { LocationPermissions(it) } }
    private var listener: MainActivityListener? = null
    private var deploymentListener: DeploymentListener? = null

    private var isFirstTime = true
    private var currentUserLocation: Location? = null

    private val analytics by lazy { context?.let { Analytics(it) } }

    private var currentSiteLoading = 0

    private lateinit var arrayListOfSite: ArrayList<String>
    private lateinit var adapterOfSearchSite: ArrayAdapter<String>

    private val mapboxLocationChangeCallback =
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                if (activity != null) {
                    val location = result?.lastLocation
                    location ?: return

                    mapboxMap?.let {
                        this@MapFragment.currentUserLocation = location
                    }
                    if (isFirstTime && locations.isNotEmpty()) {
                        moveCameraOnStart()
                        isFirstTime = false
                    }
                }
            }

            override fun onFailure(exception: Exception) {
                Log.e(LocationFragment.TAG, exception.localizedMessage ?: "empty localizedMessage")
            }
        }

    // observer
    private val deploymentWorkInfoObserve = Observer<List<WorkInfo>> {
        val currentWorkStatus = it?.getOrNull(0)
        if (currentWorkStatus != null) {
            when (currentWorkStatus.state) {
                WorkInfo.State.RUNNING -> {
                    updateSyncInfo(SyncInfo.Uploading)
                }
                WorkInfo.State.SUCCEEDED -> {
                    updateSyncInfo(SyncInfo.Uploaded)
                }
                else -> {
                    updateSyncInfo()
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.listener = context as MainActivityListener
        this.deploymentListener = context as DeploymentListener
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationPermissions?.handleActivityResult(requestCode, resultCode)
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
        context?.let { Mapbox.getInstance(it, getString(R.string.mapbox_token)) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        fetchJobSyncing()
        fetchData()
        setupSearch()
        progressBar.visibility = View.VISIBLE

        currentLocationButton.setOnClickListener {
            mapboxMap?.locationComponent?.isLocationComponentActivated?.let {
                if (it) {
                    moveCameraToCurrentLocation()
                } else {
                    mapboxMap?.style?.let { style ->
                        checkThenAccquireLocation(style)
                    }
                }
            }
        }

        zoomOutButton.setOnClickListener {
            mapboxMap?.let {
                it.animateCamera(CameraUpdateFactory.zoomOut(), DURATION)
            }
        }

        zoomInButton.setOnClickListener {
            mapboxMap?.let {
                it.animateCamera(CameraUpdateFactory.zoomIn(), DURATION)
            }
        }

        menuImageView.setOnClickListener {
            val projectName = listener?.getProjectName() ?: getString(R.string.none)
            context?.let { context ->
                LocationGroupActivity.startActivity(
                    context,
                    projectName,
                    Screen.MAP.id,
                    REQUEST_CODE
                )
            }
        }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (arrayListOfSite.contains(query)) {
                    adapterOfSearchSite.filter.filter(query)
                } else {
                    Toast.makeText(context, R.string.no_result_found, Toast.LENGTH_LONG).show()
                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                adapterOfSearchSite.filter.filter(newText)
                return false
            }
        })

        searchView.setOnSearchClickListener {
            listView.visibility = View.VISIBLE
            hideButtonOnMap()
            projectNameTextView.visibility = View.GONE
            val state = listener?.getBottomSheetState() ?: 0
            if (state == BottomSheetBehavior.STATE_EXPANDED) {
                listener?.hideBottomSheetAndBottomAppBar()
            } else {
                listener?.hidBottomAppBar()
            }
        }

        searchView.setOnCloseListener {
            listView.visibility = View.GONE
            showButtonOnMap()
            projectNameTextView.visibility = View.VISIBLE
            listener?.showBottomAppBar()
            listener?.clearFeatureSelectedOnMap()
            false
        }

        if (searchView.isIconified) {
            showButtonOnMap()
        }

        listView.setOnItemClickListener { parent, view, position, id ->
            val projectName = adapterOfSearchSite.getItem(position)

            projectName?.let { name ->
                val item = locateDb.getLocateByName(name)
                item?.let { mapboxMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(it.getLatLng(), DEFAULT_ZOOM_LEVEL)) }

                val deployment = edgeDeploymentDb.getDeploymentBySiteName(name)
                if (deployment != null) {
                    (activity as MainActivityListener).showBottomSheet(
                        DeploymentViewPagerFragment.newInstance(
                            deployment.id,
                            Device.AUDIOMOTH.value
                        )
                    )
                }
            }

            searchView.isIconified = true
            if (!searchView.isIconified) {
                searchView.isIconified = true
            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false

        context?.let {
            retrieveDeployments(it)
            retrieveLocations(it)
            retrieveProjects(it)
        }

        mapboxMap.setStyle(Style.OUTDOORS) {
            checkThenAccquireLocation(it)
            setupSources(it)
            setupImages(it)
            setupMarkerLayers(it)
            setupScale()

            mapboxMap.addOnMapClickListener { latLng ->
                handleClickIcon(mapboxMap.projection.toScreenLocation(latLng))
            }
        }
    }

    private fun setupSources(style: Style) {
        mapSource =
            GeoJsonSource(
                SOURCE_DEPLOYMENT,
                FeatureCollection.fromFeatures(listOf()),
                GeoJsonOptions()
                    .withCluster(true)
                    .withClusterMaxZoom(20)
                    .withClusterRadius(30)
                    .withClusterProperty(PROPERTY_CLUSTER_TYPE, sum(accumulated(), get(PROPERTY_CLUSTER_TYPE)), switchCase(
                        eq(get(PROPERTY_DEPLOYMENT_MARKER_IMAGE), Battery.BATTERY_PIN_GREEN), literal(1), literal(0)))
            )

        style.addSource(mapSource!!)
    }

    fun clearFeatureSelected() {
        if (this.mapFeatures?.features() != null) {
            val features = this.mapFeatures!!.features()
            features?.forEach { setFeatureSelectState(it, false) }
        }
    }

    private fun setFeatureSelectState(feature: Feature, selectedState: Boolean) {
        feature.properties()?.let {
            it.addProperty(PROPERTY_DEPLOYMENT_SELECTED, selectedState)
            refreshSource()
        }
    }

    private fun setupImages(style: Style) {
        val drawablePinSite =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map_grey, null)
        val mBitmapPinSite = BitmapUtils.getBitmapFromDrawable(drawablePinSite)
        if (mBitmapPinSite != null) {
            style.addImage(SITE_MARKER, mBitmapPinSite)
        }

        val drawablePinConnectedGuardian =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map, null)
        val mBitmapPinConnectedGuardian =
            BitmapUtils.getBitmapFromDrawable(drawablePinConnectedGuardian)
        if (mBitmapPinConnectedGuardian != null) {
            style.addImage(GuardianPin.CONNECTED_GUARDIAN, mBitmapPinConnectedGuardian)
        }

        val drawablePinNotConnectedGuardian =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map_grey, null)
        val mBitmapPinNotConnectedGuardian =
            BitmapUtils.getBitmapFromDrawable(drawablePinNotConnectedGuardian)
        if (mBitmapPinNotConnectedGuardian != null) {
            style.addImage(GuardianPin.NOT_CONNECTED_GUARDIAN, mBitmapPinNotConnectedGuardian)
        }

        val drawablePinMapGreen =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map, null)
        val mBitmapPinMapGreen = BitmapUtils.getBitmapFromDrawable(drawablePinMapGreen)
        if (mBitmapPinMapGreen != null) {
            style.addImage(Battery.BATTERY_PIN_GREEN, mBitmapPinMapGreen)
        }

        val drawablePinMapGrey =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map_grey, null)
        val mBitmapPinMapGrey = BitmapUtils.getBitmapFromDrawable(drawablePinMapGrey)
        if (mBitmapPinMapGrey != null) {
            style.addImage(Battery.BATTERY_PIN_GREY, mBitmapPinMapGrey)
        }
    }

    private fun setupMarkerLayers(style: Style) {

        val unclusteredSiteLayer =
            SymbolLayer(MARKER_SITE_ID, SOURCE_DEPLOYMENT).withProperties(
                iconImage("{$PROPERTY_SITE_MARKER_IMAGE}"),
                iconSize(0.8f),
                iconAllowOverlap(true)
            )

        val unclusteredDeploymentLayer =
            SymbolLayer(MARKER_DEPLOYMENT_ID, SOURCE_DEPLOYMENT).withProperties(
                iconImage("{$PROPERTY_DEPLOYMENT_MARKER_IMAGE}"),
                iconSize(
                    match(
                        toString(
                            get(
                                PROPERTY_DEPLOYMENT_SELECTED
                            )
                        ), literal(0.8f), stop("true", 1.0f)
                    )
                ),
                iconAllowOverlap(true)
            )

        style.addLayer(unclusteredSiteLayer)
        style.addLayer(unclusteredDeploymentLayer)

        val layers = arrayOf(intArrayOf(0, Color.parseColor("#98A0A9")), intArrayOf(1, Color.parseColor("#2AA841")))

        layers.forEachIndexed { i, ly ->
            val deploymentSymbolLayer = CircleLayer("$DEPLOYMENT_CLUSTER-$i", SOURCE_DEPLOYMENT)
            val hasDeploymentAtLeastOne = toNumber(get(PROPERTY_CLUSTER_TYPE))
            val pointCount = toNumber(get(POINT_COUNT))
            deploymentSymbolLayer.setProperties(circleColor(ly[1]), circleRadius(16f))
            deploymentSymbolLayer.setFilter(
                if (i == 0) {
                    all(
                        gte(hasDeploymentAtLeastOne, literal(ly[0])),
                        gte(pointCount, literal(1))
                    )
                } else {
                    all(
                        gte(hasDeploymentAtLeastOne, literal(ly[0])),
                        gt(hasDeploymentAtLeastOne, literal(layers[i-1][0]))
                    )
                }
            )

            style.addLayer(deploymentSymbolLayer)
        }

        val deploymentCount = SymbolLayer(DEPLOYMENT_COUNT, SOURCE_DEPLOYMENT)
        deploymentCount.setProperties(
            textField(format(formatEntry(toString(get(POINT_COUNT)), FormatOption.formatFontScale(1.5)))),
            textSize(12f),
            textColor(Color.WHITE),
            textIgnorePlacement(true),
            textOffset(arrayOf(0f, -0.2f)),
            textAllowOverlap(true)
        )

        style.addLayer(deploymentCount)
    }

    private fun setupScale() {
        val scaleBarPlugin = ScaleBarPlugin(mapView, mapboxMap!!)
        scaleBarPlugin.create(ScaleBarOptions(requireContext()))
    }

    private fun handleClickIcon(screenPoint: PointF): Boolean {
        val deploymentFeatures = mapboxMap?.queryRenderedFeatures(screenPoint, MARKER_DEPLOYMENT_ID)
        val deploymentClusterFeatures =
            mapboxMap?.queryRenderedFeatures(screenPoint, "$DEPLOYMENT_CLUSTER-0")
        if (deploymentFeatures != null && deploymentFeatures.isNotEmpty()) {
            val selectedFeature = deploymentFeatures[0]
            val features = this.mapFeatures!!.features()!!
            features.forEachIndexed { index, feature ->
                if (selectedFeature.getProperty(PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID) == feature.getProperty(
                        PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID
                    )
                ) {
                    features[index]?.let { setFeatureSelectState(it, true) }
                    val deploymentId =
                        selectedFeature.getStringProperty(PROPERTY_DEPLOYMENT_MARKER_DEPLOYMENT_ID)
                            .toInt()
                    val deploymentDevice =
                        selectedFeature.getStringProperty(PROPERTY_DEPLOYMENT_MARKER_DEVICE)
                            .toString()
                    (activity as MainActivityListener).showBottomSheet(
                        DeploymentViewPagerFragment.newInstance(deploymentId, deploymentDevice)
                    )
                    analytics?.trackClickPinEvent()
                } else {
                    features[index]?.let { setFeatureSelectState(it, false) }
                }
            }
            return true
        } else {
            (activity as MainActivityListener).hideBottomSheet()
        }

        if (deploymentClusterFeatures != null && deploymentClusterFeatures.isNotEmpty()) {
            val pinCount =
                if (deploymentClusterFeatures[0].getProperty(POINT_COUNT) != null) deploymentClusterFeatures[0].getProperty(
                    POINT_COUNT
                ).asInt else 0
            if (pinCount > 0) {
                val clusterLeavesFeatureCollection =
                    mapSource?.getClusterLeaves(deploymentClusterFeatures[0], 8000, 0)
                if (clusterLeavesFeatureCollection != null) {
                    moveCameraToLeavesBounds(clusterLeavesFeatureCollection)
                }
            }
        }
        clearFeatureSelected()
        return false
    }

    private fun moveCameraToLeavesBounds(featureCollectionToInspect: FeatureCollection) {
        val latLngList: ArrayList<LatLng> = ArrayList()
        if (featureCollectionToInspect.features() != null) {
            for (singleClusterFeature in featureCollectionToInspect.features()!!) {
                val clusterPoint = singleClusterFeature.geometry() as Point?
                if (clusterPoint != null) {
                    latLngList.add(LatLng(clusterPoint.latitude(), clusterPoint.longitude()))
                }
            }
            if (latLngList.size > 1) {
                val latLngBounds = LatLngBounds.Builder()
                    .includes(latLngList)
                    .build()
                mapboxMap?.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 230), 1300)
            }
        }
    }

    private val guardianDeploymentObserve = Observer<List<GuardianDeployment>> {
        this.guardianDeployments = it
        combinedData()
    }

    private val edgeDeploymentObserve = Observer<List<EdgeDeployment>> {
        this.edgeDeployments = it
        combinedData()
    }

    private val locateObserve = Observer<List<Locate>> {
        this.locations = it
        if (DownloadStreamsWorker.isRunning()) {
            listener?.showSnackbar(requireContext().getString(R.string.sites_downloading), Snackbar.LENGTH_SHORT)
        } else {
            listener?.showSnackbar(requireContext().getString(R.string.sites_synced), Snackbar.LENGTH_SHORT)
        }
        combinedData()
    }

    private val locationGroupObserve = Observer<List<LocationGroups>> {
        this.locationGroups = it
        combinedData()
    }

    private fun combinedData() {
        // hide loading progress
        progressBar.visibility = View.INVISIBLE

        val showDeployIds = locations.mapTo(arrayListOf(), {
            it.getLastDeploymentId()
        })

        var showGuardianDeployments = this.guardianDeployments.filter {
            showDeployIds.contains(it.serverId) || showDeployIds.contains(it.id.toString())
        }

        var showDeployments = this.edgeDeployments.filter { it.isCompleted() }
        val usedSites = showDeployments.map { it.stream?.coreId }
        var filteredShowLocations =
            locations.filter { loc -> !usedSites.contains(loc.serverId) }
        val projectName = listener?.getProjectName() ?: getString(R.string.none)
        if (projectName != getString(R.string.none)) {
            filteredShowLocations =
                filteredShowLocations.filter { it.locationGroup?.name == listener?.getProjectName() }
            showDeployments =
                showDeployments.filter { it.stream?.project?.name == listener?.getProjectName() }
            showGuardianDeployments =
                showGuardianDeployments.filter { it.stream?.project?.name == listener?.getProjectName() }
        }

        val edgeDeploymentMarkers =
            showDeployments.map { it.toMark(requireContext(), locationGroupDb) }
        val guardianDeploymentMarkers = showGuardianDeployments.map { it.toMark(requireContext()) }
        val deploymentMarkers = edgeDeploymentMarkers + guardianDeploymentMarkers
        val locationMarkers = filteredShowLocations.map { it.toMark() }
        handleShowDeployment(showDeployments, showGuardianDeployments)

        handleMarker(deploymentMarkers + locationMarkers)

        if (deploymentMarkers.isNotEmpty()) {
            val lastReport = deploymentMarkers.sortedByDescending { it.updatedAt }.first()
            mapboxMap?.let {
                it.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            lastReport.latitude,
                            lastReport.longitude
                        ), it.cameraPosition.zoom
                    )
                )
            }
        }

        arrayListOfSite = ArrayList(locations.filter { loc ->
            loc.locationGroup?.name == projectName || projectName == getString(R.string.none)
        }.map { it.name })

        context?.let {
            adapterOfSearchSite = ArrayAdapter(
                it,
                android.R.layout.simple_list_item_1,
                arrayListOfSite
            )
            listView.adapter = adapterOfSearchSite
        }
    }

    private fun getFurthestSiteFromCurrentLocation(
        currentLatLng: LatLng,
        sites: List<Locate>
    ): Locate? {
        return sites.maxBy {
            currentLatLng.distanceTo(LatLng(it.latitude, it.longitude))
        }
    }

    private fun handleShowDeployment(
        edgeDeployments: List<EdgeDeployment>,
        guardianDeployments: List<GuardianDeployment>
    ) {
        val deploymentDetails = arrayListOf<DeploymentDetailView>()
        deploymentDetails.addAll(edgeDeployments.map {
            it.toEdgeDeploymentView()
        })
        deploymentDetails.addAll(guardianDeployments.map {
            it.toGuardianDeploymentView()
        })
        deploymentListener?.setShowDeployments(deploymentDetails)
    }

    private fun fetchData() {
        locateLiveData = Transformations.map(locateDb.getAllResultsAsync().asLiveData()) {
            it
        }

        edgeDeployLiveData =
            Transformations.map(edgeDeploymentDb.getAllResultsAsync().asLiveData()) {
                it
            }

        guardianDeployLiveData =
            Transformations.map(guardianDeploymentDb.getAllResultsAsync().asLiveData()) {
                it
            }

        locationGroupLiveData =
            Transformations.map(locationGroupDb.getAllResultsAsync().asLiveData()) {
                it
            }

        locateLiveData.observeForever(locateObserve)
        edgeDeployLiveData.observeForever(edgeDeploymentObserve)
        guardianDeployLiveData.observeForever(guardianDeploymentObserve)
        locationGroupLiveData.observeForever(locationGroupObserve)
    }

    private fun retrieveDeployments(context: Context) {
        val token = "Bearer ${context.getIdToken()}"
        ApiManager.getInstance().getDeviceApi().getDeployments(token)
            .enqueue(object : Callback<List<DeploymentResponse>> {
                override fun onFailure(call: Call<List<DeploymentResponse>>, t: Throwable) {
                    combinedData()
                    if (context.isNetworkAvailable()) {
                        Toast.makeText(context, R.string.error_has_occurred, Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onResponse(
                    call: Call<List<DeploymentResponse>>,
                    response: Response<List<DeploymentResponse>>
                ) {
                    response.body()?.forEach { item ->
                        if (item.deploymentType == Device.GUARDIAN.value) {
                            guardianDeploymentDb.insertOrUpdate(item)
                        } else {
                            edgeDeploymentDb.insertOrUpdate(item)
                        }
                    }
                    retrieveImages(context)
                    combinedData()
                }
            })
    }

    private fun retrieveLocations(context: Context) {
        DownloadStreamsWorker.enqueue(context)
    }

    private fun retrieveProjects(context: Context) {
        val token = "Bearer ${context.getIdToken()}"
        ApiManager.getInstance().getDeviceApi().getProjects(token)
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    combinedData()
                    if (context.isNetworkAvailable()) {
                        Toast.makeText(context, R.string.error_has_occurred, Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onResponse(
                    call: Call<List<ProjectResponse>>,
                    response: Response<List<ProjectResponse>>
                ) {
                    response.body()?.forEach { item ->
                        locationGroupDb.insertOrUpdate(item)
                    }
                    combinedData()
                }
            })
    }

    private fun retrieveDiagnostics(context: Context) {
        Firestore(context).retrieveDiagnostics(diagnosticDb)
    }

    private fun retrieveImages(context: Context) {
        val token = "Bearer ${context.getIdToken()}"

        edgeDeployments.forEach { dp ->
            if (dp.serverId != null) {
                ApiManager.getInstance().getDeviceApi().getImages(token, dp.serverId!!)
                    .enqueue(object : Callback<List<DeploymentImageResponse>> {
                        override fun onFailure(
                            call: Call<List<DeploymentImageResponse>>,
                            t: Throwable
                        ) {
                            combinedData()
                            if (context.isNetworkAvailable()) {
                                Toast.makeText(
                                    context,
                                    R.string.error_has_occurred,
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }

                        override fun onResponse(
                            call: Call<List<DeploymentImageResponse>>,
                            response: Response<List<DeploymentImageResponse>>
                        ) {
                            response.body()?.forEach { item ->
                                deploymentImageDb.insertOrUpdate(
                                    item,
                                    dp.id,
                                    Device.AUDIOMOTH.value
                                )
                            }
                        }
                    })
            }
        }
    }

    private fun fetchJobSyncing() {
        context ?: return
        deploymentWorkInfoLiveData = DeploymentSyncWorker.workInfos(requireContext())
        deploymentWorkInfoLiveData.observeForever(deploymentWorkInfoObserve)
    }

    private fun updateSyncInfo(syncInfo: SyncInfo? = null) {
        val status = syncInfo
            ?: if (context.isNetworkAvailable()) SyncInfo.Starting else SyncInfo.WaitingNetwork
        if (this.lastSyncingInfo == SyncInfo.Uploaded && status == SyncInfo.Uploaded) return

        this.lastSyncingInfo = status
        val state = listener?.getBottomSheetState() ?: 0
        if (state != BottomSheetBehavior.STATE_EXPANDED) {
            setSnackbar(status)
        }
    }

    private fun setSnackbar(status: SyncInfo) {
        val deploymentUnsentCount = edgeDeploymentDb.unsentCount().toInt()
        when (status) {
            SyncInfo.Starting, SyncInfo.Uploading -> {
                val msg = if (deploymentUnsentCount > 1) {
                    getString(R.string.format_deploys_uploading, deploymentUnsentCount.toString())
                } else {
                    getString(R.string.format_deploy_uploading)
                }
                listener?.showSnackbar(msg, Snackbar.LENGTH_SHORT)
            }
            SyncInfo.Uploaded -> {
                val msg = getString(R.string.format_deploys_uploaded)
                listener?.showSnackbar(msg, Snackbar.LENGTH_SHORT)
            }
            // else also waiting network
            else -> {
                listener?.showSnackbar(
                    getString(R.string.format_deploy_waiting_network),
                    Snackbar.LENGTH_LONG
                )
            }
        }
    }

    private fun handleMarker(
        mapMarker: List<MapMarker>
    ) {
        val deploymentFeatures = this.mapFeatures?.features()
        val deploymentSelecting = deploymentFeatures?.firstOrNull { feature ->
            feature.getBooleanProperty(PROPERTY_DEPLOYMENT_SELECTED) ?: false
        }

        // Create point
        val mapMarkerPointFeatures = mapMarker.map {
            // check is this deployment is selecting (to set bigger pin)
            when (it) {
                is MapMarker.DeploymentMarker -> {
                    val isSelecting =
                        if (deploymentSelecting == null) {
                            false
                        } else {
                            it.id.toString() == deploymentSelecting.getProperty(
                                PROPERTY_DEPLOYMENT_MARKER_DEPLOYMENT_ID
                            ).asString
                        }
                    val properties = mapOf(
                        Pair(PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID, "${it.locationName}.${it.id}"),
                        Pair(PROPERTY_DEPLOYMENT_MARKER_IMAGE, it.pin),
                        Pair(PROPERTY_DEPLOYMENT_MARKER_TITLE, it.locationName),
                        Pair(PROPERTY_DEPLOYMENT_MARKER_DEPLOYMENT_ID, it.id.toString()),
                        Pair(PROPERTY_DEPLOYMENT_MARKER_CAPTION, it.description),
                        Pair(PROPERTY_DEPLOYMENT_MARKER_DEVICE, it.device),
                        Pair(PROPERTY_DEPLOYMENT_SELECTED, isSelecting.toString())
                    )
                    Feature.fromGeometry(
                        Point.fromLngLat(it.longitude, it.latitude), properties.toJsonObject()
                    )
                }
                is MapMarker.SiteMarker -> {
                    val properties = mapOf(
                        Pair(PROPERTY_SITE_MARKER_IMAGE, it.pin)
                    )

                    Feature.fromGeometry(
                        Point.fromLngLat(it.longitude, it.latitude), properties.toJsonObject()
                    )
                }
            }
        }

        this.mapFeatures = FeatureCollection.fromFeatures(mapMarkerPointFeatures)
        refreshSource()
    }

    private fun refreshSource() {
        if (mapSource != null && mapFeatures != null) {
            mapSource!!.setGeoJson(mapFeatures)
        }
    }

    private fun moveCamera(userLoc: LatLng, targetLoc: LatLng? = null, zoom: Double) {
        mapboxMap?.moveCamera(MapboxCameraUtils.calculateLatLngForZoom(userLoc, targetLoc, zoom))
    }

    private fun checkThenAccquireLocation(style: Style) {
        locationPermissions?.check { isAllowed: Boolean ->
            if (isAllowed) {
                enableLocationComponent(style)
            }
        }
    }

    private fun enableLocationComponent(style: Style) {
        val customLocationComponentOptions = context?.let {
            LocationComponentOptions.builder(it)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(it, R.color.colorPrimary))
                .build()
        }

        val locationComponentActivationOptions =
            context?.let {
                LocationComponentActivationOptions.builder(it, style)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()
            }

        mapboxMap?.let { it ->
            it.locationComponent.apply {
                if (locationComponentActivationOptions != null) {
                    activateLocationComponent(locationComponentActivationOptions)
                }

                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.COMPASS
            }
        }

        initLocationEngine()
    }

    private fun initLocationEngine() {
        locationEngine = context?.let { LocationEngineProvider.getBestLocationEngine(it) }
        val request =
            LocationEngineRequest.Builder(LocationFragment.DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(LocationFragment.DEFAULT_MAX_WAIT_TIME).build()

        locationEngine?.requestLocationUpdates(
            request,
            mapboxLocationChangeCallback,
            Looper.getMainLooper()
        )

        locationEngine?.getLastLocation(mapboxLocationChangeCallback)
    }

    private fun moveCameraOnStart() {
        mapboxMap?.locationComponent?.lastKnownLocation?.let { curLoc ->
            val currentLatLng = LatLng(curLoc.latitude, curLoc.longitude)
            val furthestSite = getFurthestSiteFromCurrentLocation(currentLatLng, this.locations)
            furthestSite?.let {
                moveCamera(
                    currentLatLng,
                    LatLng(furthestSite.latitude, furthestSite.longitude),
                    DEFAULT_ZOOM_LEVEL
                )
            }
        }
    }

    private fun moveCameraOnStartWithProject() {
        mapboxMap?.locationComponent?.lastKnownLocation?.let { curLoc ->
            val currentLatLng = LatLng(curLoc.latitude, curLoc.longitude)
            val preferences = context?.let { Preferences.getInstance(it) }
            val projectName =
                preferences?.getString(Preferences.SELECTED_PROJECT, getString(R.string.none))
            val locations = this.locations.filter { it.locationGroup?.name == projectName }
            val furthestSite = getFurthestSiteFromCurrentLocation(
                currentLatLng,
                if (projectName != getString(R.string.none)) locations else this.locations
            )
            furthestSite?.let {
                moveCamera(
                    currentLatLng,
                    LatLng(furthestSite.latitude, furthestSite.longitude),
                    DEFAULT_ZOOM_LEVEL
                )
            }
        }
    }

    private fun moveCameraToCurrentLocation() {
        mapboxMap?.locationComponent?.lastKnownLocation?.let { curLoc ->
            val currentLatLng = LatLng(curLoc.latitude, curLoc.longitude)
            moveCamera(
                currentLatLng,
                null,
                mapboxMap?.cameraPosition?.zoom ?: DEFAULT_ZOOM_LEVEL
            )
        }
    }

    fun moveToDeploymentMarker(lat: Double, lng: Double, markerLocationId: String) {
        mapboxMap?.let {
            it.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), DEFAULT_ZOOM_LEVEL)
            )
        }

        val features = this.mapFeatures!!.features()!!
        features.forEachIndexed { index, feature ->
            feature.getProperty(PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID)?.let { property ->
                if (markerLocationId == property.toString()
                ) {
                    features[index]?.let { setFeatureSelectState(it, true) }
                } else {
                    features[index]?.let { setFeatureSelectState(it, false) }
                }
            }
        }
    }

    fun showButtonOnMap() {
        buttonOnMapGroup.visibility = View.VISIBLE
    }

    fun hideButtonOnMap() {
        buttonOnMapGroup.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        listener?.let { it ->
            projectNameTextView.text =
                if (it.getProjectName() != getString(R.string.none)) it.getProjectName() else ""
            combinedData()
            mapboxMap?.locationComponent?.isLocationComponentActivated?.let { isActivated ->
                if (isActivated) {
                    moveCameraOnStartWithProject()
                }
            }
        }
        analytics?.trackScreen(Screen.MAP)
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
        deploymentWorkInfoLiveData.removeObserver(deploymentWorkInfoObserve)
        guardianDeployLiveData.removeObserver(guardianDeploymentObserve)
        edgeDeployLiveData.removeObserver(edgeDeploymentObserve)
        locateLiveData.removeObserver(locateObserve)
        locationGroupLiveData.removeObserver(locationGroupObserve)
        locationEngine?.removeLocationUpdates(mapboxLocationChangeCallback)
        mapView.onDestroy()
    }

    companion object {
        const val tag = "MapFragment"
        const val SITE_MARKER = "SITE_MARKER"
        private const val DEFAULT_ZOOM_LEVEL = 15.0

        private const val SOURCE_DEPLOYMENT = "source.deployment"
        private const val MARKER_DEPLOYMENT_ID = "marker.deployment"
        private const val MARKER_SITE_ID = "marker.site"

        private const val PROPERTY_DEPLOYMENT_SELECTED = "deployment.selected"
        private const val PROPERTY_DEPLOYMENT_MARKER_DEVICE = "deployment.device"
        private const val PROPERTY_DEPLOYMENT_MARKER_LOCATION_ID = "deployment.location"
        private const val PROPERTY_DEPLOYMENT_MARKER_TITLE = "deployment.title"
        private const val PROPERTY_DEPLOYMENT_MARKER_DEPLOYMENT_ID = "deployment.deployment"
        private const val PROPERTY_DEPLOYMENT_MARKER_CAPTION = "deployment.caption"
        private const val PROPERTY_DEPLOYMENT_MARKER_IMAGE = "deployment.marker.image"

        private const val PROPERTY_SITE_MARKER_IMAGE = "site.marker.image"

        private const val PROPERTY_CLUSTER_TYPE = "cluster.type"

        private const val DEPLOYMENT_CLUSTER = "deployment.cluster"
        private const val POINT_COUNT = "point_count"
        private const val DEPLOYMENT_COUNT = "deployment.count"

        private const val DURATION = 700
        const val REQUEST_CODE = 1006

        fun newInstance(): MapFragment {
            return MapFragment()
        }
    }
}
