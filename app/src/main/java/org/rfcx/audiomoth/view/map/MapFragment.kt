package org.rfcx.audiomoth.view.map


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.work.WorkInfo
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.BubbleLayout
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
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.layout_map_window_info.view.*
import org.rfcx.audiomoth.MainActivityListener
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.DeploymentState.AudioMoth
import org.rfcx.audiomoth.entity.Device
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.localdb.guardian.GuardianDeploymentDb
import org.rfcx.audiomoth.repo.Firestore
import org.rfcx.audiomoth.service.DeploymentSyncWorker
import org.rfcx.audiomoth.util.*
import org.rfcx.audiomoth.view.deployment.DeploymentActivity

class MapFragment : Fragment(), OnMapReadyCallback {
    // map
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private var deploymentSource: GeoJsonSource? = null
    private var deploymentFeatures: FeatureCollection? = null
    private val mapInfoViews = hashMapOf<String, View>()

    // database manager
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentDb by lazy { DeploymentDb(realm) }
    private val guardianDeploymentDb by lazy { GuardianDeploymentDb(realm) }
    private val locateDb by lazy { LocateDb(realm) }

    // data
    private var guardianDeployments = listOf<GuardianDeployment>()
    private var deployments = listOf<Deployment>()
    private var locations = listOf<Locate>()
    private var lastSyncingInfo: SyncInfo? = null

    private lateinit var guardianDeployLiveData: LiveData<List<GuardianDeployment>>
    private lateinit var deployLiveData: LiveData<List<Deployment>>
    private lateinit var locateLiveData: LiveData<List<Locate>>
    private lateinit var deploymentWorkInfoLiveData: LiveData<List<WorkInfo>>

    private val locationPermissions by lazy { activity?.let { LocationPermissions(it) } }
    private var listener: MainActivityListener? = null

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
        inflater: LayoutInflater, container: ViewGroup?,
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
        progressBar.visibility = View.VISIBLE
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.OUTDOORS) {
            checkThenAccquireLocation(it)
            setupSources(it)
            setupImages(it)
            setupMarkerLayers(it)
            setupWindowInfo(it)

            fetchData()
            mapboxMap.addOnMapClickListener { latLng ->
                val screenPoint = mapboxMap.projection.toScreenLocation(latLng)
                val features = mapboxMap.queryRenderedFeatures(screenPoint, WINDOW_DEPLOYMENT_ID)
                if (features.isNotEmpty()) {
                    val feature = features[0]
                    val device = feature.getStringProperty(PROPERTY_MARKER_DEVICE)
                    if (device == Device.EDGE.value) {
                        handlePressedDeployment(feature)
                    } else {
                        handlePressedGuardianDeployment(feature)
                    }
                    true
                } else {
                    handleClickIcon(mapboxMap.projection.toScreenLocation(latLng))
                }
            }
        }
    }

    private fun setupSources(style: Style) {
        deploymentSource =
            GeoJsonSource(SOURCE_DEPLOYMENT, FeatureCollection.fromFeatures(listOf()))
        style.addSource(deploymentSource!!)
    }

    private fun setupImages(style: Style) {
        val drawablePinGuardian =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map, null)
        val mBitmapPinGuardian = BitmapUtils.getBitmapFromDrawable(drawablePinGuardian)
        if (mBitmapPinGuardian != null) {
            style.addImage(MARKER_GUARDIAN_PIN, mBitmapPinGuardian)
        }

        val drawablePinMapGreen =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map, null)
        val mBitmapPinMapGreen = BitmapUtils.getBitmapFromDrawable(drawablePinMapGreen)
        if (mBitmapPinMapGreen != null) {
            style.addImage(Battery.BATTERY_PIN_GREEN, mBitmapPinMapGreen)
        }

        val drawablePinMapOrange =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map_orange, null)
        val mBitmapPinMapOrange = BitmapUtils.getBitmapFromDrawable(drawablePinMapOrange)
        if (mBitmapPinMapOrange != null) {
            style.addImage(Battery.BATTERY_PIN_ORANGE, mBitmapPinMapOrange)
        }

        val drawablePinMapRed =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map_red, null)
        val mBitmapPinMapRed = BitmapUtils.getBitmapFromDrawable(drawablePinMapRed)
        if (mBitmapPinMapRed != null) {
            style.addImage(Battery.BATTERY_PIN_RED, mBitmapPinMapRed)
        }

        val drawablePinMapGrey =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map_grey, null)
        val mBitmapPinMapGrey = BitmapUtils.getBitmapFromDrawable(drawablePinMapGrey)
        if (mBitmapPinMapGrey != null) {
            style.addImage(Battery.BATTERY_PIN_GREY, mBitmapPinMapGrey)
        }
    }

    private fun setupWindowInfo(it: Style) {
        it.addLayer(SymbolLayer(WINDOW_DEPLOYMENT_ID, SOURCE_DEPLOYMENT).apply {
            withProperties(
                PropertyFactory.iconImage("{${PROPERTY_MARKER_LOCATION_ID}}"),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconOffset(arrayOf(-2f, -20f)),
                PropertyFactory.iconAllowOverlap(true)
            )
            withFilter(Expression.eq(Expression.get(PROPERTY_SELECTED), Expression.literal(true)))
        })
    }

    private fun setupMarkerLayers(style: Style) {
        val markerLayer = SymbolLayer(MARKER_DEPLOYMENT_ID, SOURCE_DEPLOYMENT).apply {
            withProperties(
                PropertyFactory.iconImage("{${PROPERTY_MARKER_IMAGE}}"),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconSize(1f)
            )
        }
        style.addLayer(markerLayer)
    }

    private fun handlePressedGuardianDeployment(feature: Feature) {
        Toast.makeText(context, "Pressed GuardianDeployment", Toast.LENGTH_SHORT).show()
    }

    private fun handlePressedDeployment(feature: Feature) {
        val deployment =
            deploymentDb.getDeploymentById(feature.getProperty(PROPERTY_MARKER_DEPLOYMENT_ID).asInt)
        if (deployment != null) {
            if (deployment.state != 6) {
                context?.let {
                    DeploymentActivity.startActivity(
                        it,
                        feature.getProperty(PROPERTY_MARKER_DEPLOYMENT_ID).asInt
                    )
                }
            }
        }
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
                    features[index]?.let { setFeatureSelectState(it, true) }
                } else {
                    features[index]?.let { setFeatureSelectState(it, false) }
                }
            }
            return true
        }

        clearFeatureSelected()
        return false
    }

    private fun clearFeatureSelected() {
        if (this.deploymentFeatures?.features() != null) {
            val features = this.deploymentFeatures!!.features()
            features?.forEach { setFeatureSelectState(it, false) }
        }
    }

    private fun setFeatureSelectState(feature: Feature, selectedState: Boolean) {
        feature.properties()?.let {
            it.addProperty(PROPERTY_SELECTED, selectedState)
            refreshSource()
        }
    }

    private val guardianDeploymentObserve = Observer<List<GuardianDeployment>> {
        this.guardianDeployments = it
        combinedData()
    }

    private val deploymentObserve = Observer<List<Deployment>> {
        this.deployments = it
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

        val showDeployments = this.deployments.filter {
            showDeployIds.contains(it.serverId) || showDeployIds.contains(it.id.toString())
        }

        val showGuardianDeployments = this.guardianDeployments.filter {
            showDeployIds.contains(it.serverId) || showDeployIds.contains(it.id.toString())
        }

        val deploymentMarkers = showDeployments.map { it.toMark() }
        val guardianDeploymentMarkers = showGuardianDeployments.map { it.toMark() }

        handleMarkerDeployment(deploymentMarkers + guardianDeploymentMarkers)
    }

    private fun fetchData() {
        guardianDeployLiveData =
            Transformations.map(guardianDeploymentDb.getAllResultsAsync().asLiveData()) {
                it
            }
        guardianDeployLiveData.observeForever(guardianDeploymentObserve)

        deployLiveData = Transformations.map(deploymentDb.getAllResultsAsync().asLiveData()) {
            it
        }
        deployLiveData.observeForever(deploymentObserve)

        locateLiveData = Transformations.map(locateDb.getAllResultsAsync().asLiveData()) {
            it
        }
        locateLiveData.observeForever(locateObserve)

        context?.let {
            retrieveDeployments(it)
            retrieveLocations(it)
        }
    }

    private fun retrieveDeployments(context: Context) {
        Firestore(context).retrieveDeployments(deploymentDb, guardianDeploymentDb)
    }

    private fun retrieveLocations(context: Context) {
        Firestore(context).retrieveLocations(locateDb)
    }

    private fun fetchJobSyncing() {
        context ?: return
        deploymentWorkInfoLiveData = DeploymentSyncWorker.workInfos(context!!)
        deploymentWorkInfoLiveData.observeForever(workInfoObserve)
    }

    private fun updateSyncInfo(syncInfo: SyncInfo? = null) {
        val status = syncInfo
            ?: if (context.isNetworkAvailable()) SyncInfo.Starting else SyncInfo.WaitingNetwork
        if (this.lastSyncingInfo == SyncInfo.Uploaded && status == SyncInfo.Uploaded) return

        this.lastSyncingInfo = status

        val deploymentUnsentCount = deploymentDb.unsentCount().toInt()
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
                val msg = if (deploymentUnsentCount > 1) {
                    getString(
                        R.string.format_deploys_waiting_network,
                        deploymentUnsentCount.toString()
                    )
                } else {
                    getString(R.string.format_deploy_waiting_network)
                }
                listener?.showSnackbar(msg, Snackbar.LENGTH_LONG)
            }
        }
    }

    private fun handleMarkerDeployment(deploymentMarkers: List<DeploymentMarker>) {
        // Create point
        val pointFeatures = deploymentMarkers.map {
            val properties = mapOf(
                Pair(PROPERTY_MARKER_LOCATION_ID, it.locationName),
                Pair(PROPERTY_MARKER_IMAGE, it.pin),
                Pair(PROPERTY_MARKER_TITLE, it.locationName),
                Pair(PROPERTY_MARKER_DEPLOYMENT_ID, it.id.toString()),
                Pair(PROPERTY_MARKER_CAPTION, it.description),
                Pair(PROPERTY_MARKER_DEVICE, Device.EDGE.value)
            )
            Feature.fromGeometry(
                Point.fromLngLat(it.longitude, it.latitude), properties.toJsonObject()
            )
        }

        // Create window info
        val windowInfoImages = hashMapOf<String, Bitmap>()
        val inflater = LayoutInflater.from(context)
        pointFeatures.forEach {
            val bubbleLayout =
                inflater.inflate(R.layout.layout_map_window_info, null) as BubbleLayout

            val id = it.getStringProperty(PROPERTY_MARKER_LOCATION_ID)

            val title = it.getStringProperty(PROPERTY_MARKER_TITLE)
            bubbleLayout.infoWindowTitle.text = title

            val caption = it.getStringProperty(PROPERTY_MARKER_CAPTION)
            bubbleLayout.infoWindowDescription.text = caption

            val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            bubbleLayout.measure(measureSpec, measureSpec)
            val measuredWidth = bubbleLayout.measuredWidth
            bubbleLayout.arrowPosition = (measuredWidth / 2 - 5).toFloat()

            val bitmap = SymbolGenerator.generate(bubbleLayout)
            mapInfoViews[id] = bubbleLayout
            windowInfoImages[id] = bitmap
        }

        setWindowInfoImageGenResults(windowInfoImages)
        deploymentFeatures = FeatureCollection.fromFeatures(pointFeatures)
        refreshSource()

        val lastDeployment = deployments.lastOrNull()
        if (lastDeployment?.location != null) {
            moveCamera(
                LatLng(
                    lastDeployment.location!!.latitude,
                    lastDeployment.location!!.longitude
                )
            )
        }
    }

    private fun refreshSource() {
        if (deploymentSource != null && deploymentFeatures != null) {
            deploymentSource!!.setGeoJson(deploymentFeatures)
        }
    }

    private fun setWindowInfoImageGenResults(windowInfoImages: HashMap<String, Bitmap>) {
        mapboxMap?.style?.addImages(windowInfoImages)
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
        deployLiveData.removeObserver(deploymentObserve)
        locateLiveData.removeObserver(locateObserve)
        mapView.onDestroy()
    }

    private fun Deployment.toMark(): DeploymentMarker {
        val pinImage = if (state == AudioMoth.ReadyToUpload.key)
            Battery.getBatteryPinImage(batteryDepletedAt.time)
        else
            Battery.BATTERY_PIN_GREY

        val description = if (state >= AudioMoth.ReadyToUpload.key)
            Battery.getPredictionBattery(batteryDepletedAt.time)
        else
            getString(R.string.format_in_progress_step)

        return DeploymentMarker(
            id, location?.name ?: "",
            location?.longitude ?: 0.0,
            location?.latitude ?: 0.0,
            pinImage, description, Device.EDGE.value
        )
    }

    private fun GuardianDeployment.toMark(): DeploymentMarker {
        return DeploymentMarker(
            id,
            location?.name ?: "",
            location?.longitude ?: 0.0,
            location?.latitude ?: 0.0,
            MARKER_GUARDIAN_PIN,
            "-",
            Device.GUARDIAN.value
        )
    }

    companion object {
        const val tag = "MapFragment"

        private const val SOURCE_DEPLOYMENT = "source.deployment"
        private const val MARKER_DEPLOYMENT_ID = "marker.deployment"
        private const val WINDOW_DEPLOYMENT_ID = "info.deployment"

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
