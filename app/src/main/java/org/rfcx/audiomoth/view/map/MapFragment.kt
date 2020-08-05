package org.rfcx.audiomoth.view.map

import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.work.WorkInfo
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_map.*
import org.rfcx.audiomoth.DeploymentListener
import org.rfcx.audiomoth.MainActivityListener
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.DeploymentState.Edge
import org.rfcx.audiomoth.entity.Device
import org.rfcx.audiomoth.entity.EdgeDeployment
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment
import org.rfcx.audiomoth.localdb.EdgeDeploymentDb
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.localdb.guardian.DiagnosticDb
import org.rfcx.audiomoth.localdb.guardian.GuardianDeploymentDb
import org.rfcx.audiomoth.repo.Firestore
import org.rfcx.audiomoth.service.DeploymentSyncWorker
import org.rfcx.audiomoth.util.*

class MapFragment : Fragment(), OnMapReadyCallback {

    // map
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private var deploymentSource: GeoJsonSource? = null
    private var deploymentFeatures: FeatureCollection? = null

    // database manager
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val guardianDeploymentDb by lazy { GuardianDeploymentDb(realm) }
    private val locateDb by lazy { LocateDb(realm) }
    private val diagnosticDb by lazy { DiagnosticDb(realm) }

    // data
    private var guardianDeployments = listOf<GuardianDeployment>()
    private var edgeDeployments = listOf<EdgeDeployment>()
    private var locations = listOf<Locate>()
    private var lastSyncingInfo: SyncInfo? = null

    private lateinit var guardianDeployLiveData: LiveData<List<GuardianDeployment>>
    private lateinit var deployLiveData: LiveData<List<EdgeDeployment>>
    private lateinit var locateLiveData: LiveData<List<Locate>>
    private lateinit var deploymentWorkInfoLiveData: LiveData<List<WorkInfo>>

    private val locationPermissions by lazy { activity?.let { LocationPermissions(it) } }
    private var listener: MainActivityListener? = null
    private var deploymentListener: DeploymentListener? = null

    // observer
    private val workInfoObserve = Observer<List<WorkInfo>> {
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
        progressBar.visibility = View.VISIBLE
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false

        context?.let {
            retrieveDeployments(it)
            retrieveLocations(it)
            retrieveDiagnostics(it)
        }

        mapboxMap.setStyle(Style.OUTDOORS) {
            checkThenAccquireLocation(it)
            setupSources(it)
            setupImages(it)
            setupMarkerLayers(it)

            mapboxMap.addOnMapClickListener { latLng ->
                handleClickIcon(mapboxMap.projection.toScreenLocation(latLng))
            }
        }
    }

    private fun setupSources(style: Style) {
        deploymentSource =
            GeoJsonSource(SOURCE_DEPLOYMENT, FeatureCollection.fromFeatures(listOf()))
        style.addSource(deploymentSource!!)
    }

    private fun setupImages(style: Style) {
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
        val markerLayer = SymbolLayer(MARKER_DEPLOYMENT_ID, SOURCE_DEPLOYMENT).apply {
            withProperties(
                PropertyFactory.iconImage("{$PROPERTY_MARKER_IMAGE}"),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconSize(1f)
            )
        }
        style.addLayer(markerLayer)
    }

    private fun handleClickIcon(screenPoint: PointF): Boolean {
        val deploymentFeatures = mapboxMap?.queryRenderedFeatures(screenPoint, MARKER_DEPLOYMENT_ID)
        if (deploymentFeatures != null && deploymentFeatures.isNotEmpty()) {
            val selectedFeature = deploymentFeatures[0]
            val features = this.deploymentFeatures!!.features()!!
            features.forEachIndexed { index, feature ->
                if (selectedFeature.getProperty(PROPERTY_MARKER_LOCATION_ID) == feature.getProperty(
                        PROPERTY_MARKER_LOCATION_ID
                    )
                ) {
                    val id =
                        selectedFeature.getStringProperty(PROPERTY_MARKER_LOCATION_ID).split(".")[1]
                    (activity as MainActivityListener).showBottomSheet(
                        DeploymentViewPagerFragment.newInstance(
                            id.toInt()
                        )
                    )
                }
            }
            return true
        }
        (activity as MainActivityListener).hideBottomSheet()

        return false
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
        combinedData()
    }

    private fun combinedData() {
        // hide loading progress
        progressBar.visibility = View.INVISIBLE

        val showLocations = locations.filter { it.isCompleted() }
        val showDeployIds = showLocations.mapTo(arrayListOf(), {
            it.getLastDeploymentId()
        })

        val showDeployments = this.edgeDeployments.filter {
            if (it.isSent()) showDeployIds.contains(it.serverId) else showDeployIds.contains(it.id.toString())
        }

        val showGuardianDeployments = this.guardianDeployments.filter {
            if (it.isSent()) showDeployIds.contains(it.serverId) else showDeployIds.contains(it.id.toString())
        }

        val deploymentMarkers = showDeployments.map { it.toMark() }
        val guardianDeploymentMarkers = showGuardianDeployments.map { it.toMark() }

        handleMarkerDeployment(deploymentMarkers + guardianDeploymentMarkers)
        handleShowDeployment(showDeployments, showGuardianDeployments)
    }

    private fun handleShowDeployment(
        deployments: List<EdgeDeployment>,
        guardianDeployments: List<GuardianDeployment>
    ) {
        val showDeployments = arrayListOf<DeploymentBottomSheet>()
        val showGuardianDeployments = arrayListOf<DeploymentBottomSheet>()

        deployments.forEach {
            showDeployments.add(DeploymentBottomSheet(it.id, Device.EDGE.value))
        }

        guardianDeployments.forEach {
            showGuardianDeployments.add(DeploymentBottomSheet(it.id, Device.GUARDIAN.value))
        }

        deploymentListener?.setShowDeployments(showDeployments + showGuardianDeployments)
    }

    private fun fetchData() {
        guardianDeployLiveData =
            Transformations.map(guardianDeploymentDb.getAllResultsAsync().asLiveData()) {
                it
            }
        guardianDeployLiveData.observeForever(guardianDeploymentObserve)

        deployLiveData = Transformations.map(edgeDeploymentDb.getAllResultsAsync().asLiveData()) {
            it
        }
        deployLiveData.observeForever(edgeDeploymentObserve)

        locateLiveData = Transformations.map(locateDb.getAllResultsAsync().asLiveData()) {
            it
        }
        locateLiveData.observeForever(locateObserve)
    }

    private fun retrieveDeployments(context: Context) {
        Firestore(context).retrieveDeployments(edgeDeploymentDb, guardianDeploymentDb)
    }

    private fun retrieveLocations(context: Context) {
        Firestore(context).retrieveLocations(locateDb)
    }

    private fun retrieveDiagnostics(context: Context) {
        Firestore(context).retrieveDiagnostics(diagnosticDb)
    }

    private fun fetchJobSyncing() {
        context ?: return
        deploymentWorkInfoLiveData = DeploymentSyncWorker.workInfos(requireContext())
        deploymentWorkInfoLiveData.observeForever(workInfoObserve)
    }

    private fun updateSyncInfo(syncInfo: SyncInfo? = null) {
        val status = syncInfo
            ?: if (context.isNetworkAvailable()) SyncInfo.Starting else SyncInfo.WaitingNetwork
        if (this.lastSyncingInfo == SyncInfo.Uploaded && status == SyncInfo.Uploaded) return

        this.lastSyncingInfo = status
        val state = listener?.getBottomSheetState() ?: 0
        if (state != BottomSheetBehavior.STATE_EXPANDED){
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
                listener?.showSnackbar(msg, Snackbar.LENGTH_INDEFINITE)
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

    private fun handleMarkerDeployment(deploymentMarkers: List<DeploymentMarker>) {
        // Create point
        val pointFeatures = deploymentMarkers.map {
            val properties = mapOf(
                Pair(PROPERTY_MARKER_LOCATION_ID, "${it.locationName}.${it.id}"),
                Pair(PROPERTY_MARKER_IMAGE, it.pin),
                Pair(PROPERTY_MARKER_TITLE, it.locationName),
                Pair(PROPERTY_MARKER_DEPLOYMENT_ID, it.id.toString()),
                Pair(PROPERTY_MARKER_CAPTION, it.description),
                Pair(PROPERTY_MARKER_DEVICE, it.device)
            )
            Feature.fromGeometry(
                Point.fromLngLat(it.longitude, it.latitude), properties.toJsonObject()
            )
        }

        deploymentFeatures = FeatureCollection.fromFeatures(pointFeatures)
        refreshSource()

        val lastDeployment = deploymentMarkers.maxBy { it.updatedAt ?: it.createdAt }
        if (lastDeployment != null) {
            moveCamera(LatLng(lastDeployment.latitude, lastDeployment.longitude))
        }
    }

    private fun refreshSource() {
        if (deploymentSource != null && deploymentFeatures != null) {
            deploymentSource!!.setGeoJson(deploymentFeatures)
        }
    }

    private fun moveCamera(latLng: LatLng) {
        mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0))
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
    }

    fun moveToDeploymentMarker(location: DeploymentLocation) {
        mapboxMap?.let {
            it.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        location.latitude,
                        location.longitude
                    ), it.cameraPosition.zoom
                )
            )
        }
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
        deploymentWorkInfoLiveData.removeObserver(workInfoObserve)
        guardianDeployLiveData.removeObserver(guardianDeploymentObserve)
        deployLiveData.removeObserver(edgeDeploymentObserve)
        locateLiveData.removeObserver(locateObserve)
        mapView.onDestroy()
    }

    private fun EdgeDeployment.toMark(): DeploymentMarker {
        val pinImage =
            if (state == Edge.ReadyToUpload.key && isBatteryRemaining(batteryDepletedAt.time))
                Battery.BATTERY_PIN_GREEN
            else
                Battery.BATTERY_PIN_GREY

        val description = if (state >= Edge.ReadyToUpload.key)
            Battery.getPredictionBattery(batteryDepletedAt.time)
        else
            getString(R.string.format_in_progress_step)

        return DeploymentMarker(
            id, location?.name ?: "",
            location?.longitude ?: 0.0,
            location?.latitude ?: 0.0,
            pinImage, description, Device.EDGE.value, createdAt, updatedAt
        )
    }

    private fun GuardianDeployment.toMark(): DeploymentMarker {
        val pinImage = GuardianPin.getGuardianPinImage(requireContext(), this.wifiName!!)
        return DeploymentMarker(
            id,
            location?.name ?: "",
            location?.longitude ?: 0.0,
            location?.latitude ?: 0.0,
            pinImage,
            "-",
            Device.GUARDIAN.value,
            createdAt,
            null
        )
    }

    private fun isBatteryRemaining(timestamp: Long): Boolean {
        val currentMillis = System.currentTimeMillis()
        return timestamp > currentMillis
    }

    companion object {
        const val tag = "MapFragment"

        private const val SOURCE_DEPLOYMENT = "source.deployment"
        private const val MARKER_DEPLOYMENT_ID = "marker.deployment"

        private const val PROPERTY_SELECTED = "selected"
        private const val PROPERTY_MARKER_DEVICE = "device"
        private const val PROPERTY_MARKER_LOCATION_ID = "location"
        private const val PROPERTY_MARKER_TITLE = "title"
        private const val PROPERTY_MARKER_DEPLOYMENT_ID = "deployment"
        private const val PROPERTY_MARKER_CAPTION = "caption"
        private const val PROPERTY_MARKER_IMAGE = "marker.image"
        private const val MARKER_GUARDIAN_PIN = "guardian_pin"

        fun newInstance(): MapFragment {
            return MapFragment()
        }
    }
}
