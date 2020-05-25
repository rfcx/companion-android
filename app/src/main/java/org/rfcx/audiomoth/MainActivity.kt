package org.rfcx.audiomoth

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.BubbleLayout
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_map_window_info.view.*
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.util.*
import org.rfcx.audiomoth.view.DeploymentActivity

open class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mapboxMap: MapboxMap? = null
    private var deployments = listOf<Deployment>()
    private var deploymentSource: GeoJsonSource? = null
    private var deploymentFeatures: FeatureCollection? = null
    private val locationPermissions by lazy { LocationPermissions(this) }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationPermissions.handleActivityResult(requestCode, resultCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissions.handleRequestResult(requestCode, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_token))
        setContentView(R.layout.activity_main)

        setView(savedInstanceState)
        setSyncImage()
    }

    private fun setSyncImage() {
        val images = intent.extras?.getStringArrayList(IMAGES)
        val deploymentId = intent.extras?.getString(DEPLOYMENT_ID)
        if (!images.isNullOrEmpty()) {
            if (deploymentId != null) {
                Storage(this).uploadImage(images, deploymentId) { count, unSyncNum ->
                    if (count == 1) {
                        if (unSyncNum == 0) {
                            photoSyncTextView.visibility = View.GONE
                        } else {
                            photoSyncTextView.visibility = View.VISIBLE
                            photoSyncTextView.text = getString(
                                R.string.format_image_unsync,
                                count.toString(),
                                unSyncNum.toString()
                            )
                        }
                    } else {
                        if (unSyncNum == 0) {
                            photoSyncTextView.visibility = View.GONE
                        } else {
                            photoSyncTextView.visibility = View.VISIBLE
                            photoSyncTextView.text = getString(
                                R.string.format_images_unsync,
                                count.toString(),
                                unSyncNum.toString()
                            )
                        }
                    }
                }
            }
        } else {
            photoSyncTextView.visibility = View.GONE
        }
    }

    private fun checkThenAccquireLocation(style: Style) {
        locationPermissions.check { isAllowed: Boolean ->
            if (isAllowed) {
                enableLocationComponent(style)
            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.OUTDOORS) {
            checkThenAccquireLocation(it)
            setupSources(it)
            setupImages(it)
            setupMarkerLayers(it)
            setupWindowInfo(it)

            getDeployments()
            mapboxMap.addOnMapClickListener { latLng ->
                handleClickIcon(mapboxMap.projection.toScreenLocation(latLng))
            }
        }
    }

    private fun setView(savedInstanceState: Bundle?) {
        setCreateLocationButton(false)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        createLocationButton.setOnClickListener {
            DeploymentActivity.startActivity(this)
        }
    }

    private fun setupSources(style: Style) {
        deploymentSource =
            GeoJsonSource(SOURCE_DEPLOYMENT, FeatureCollection.fromFeatures(listOf()))
        style.addSource(deploymentSource!!)
    }

    private fun refreshSource() {
        if (deploymentSource != null && deploymentFeatures != null) {
            deploymentSource!!.setGeoJson(deploymentFeatures)
        }
    }


    private fun setCreateLocationButton(show: Boolean) {
        if (show) createLocationButton.show() else createLocationButton.hide()
        progressBar.visibility = if (!show) View.VISIBLE else View.GONE
    }

    private fun enableLocationComponent(style: Style) {
        val customLocationComponentOptions = LocationComponentOptions.builder(this)
            .trackingGesturesManagement(true)
            .accuracyColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .build()

        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(this, style)
                .locationComponentOptions(customLocationComponentOptions)
                .build()

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

    private fun getDeployments() {
        Firestore(this).getDeployments(object : FirestoreResponseCallback<List<Deployment>> {
            override fun onSuccessListener(response: List<Deployment>) {
                handleMarkerDeployment(response)
            }

            override fun addOnFailureListener(exception: Exception) {
                setCreateLocationButton(true)
            }
        })
    }

    private fun handleMarkerDeployment(deployments: List<Deployment>) {
        this.deployments = deployments
        // Create point
        val pointFeatures = deployments.map {
            val properties = mapOf(
                Pair(PROPERTY_MARKER_LOCATION_ID, it.location.id),
                Pair(PROPERTY_MARKER_IMAGE, Battery.getBatteryPinImage(it.batteryDepletedAt.time)),
                Pair(PROPERTY_MARKER_TITLE, it.location.name),
                Pair(
                    PROPERTY_MARKER_CAPTION,
                    Battery.getPredictionBattery(it.batteryDepletedAt.time)
                )
            )
            Feature.fromGeometry(
                fromLngLat(it.location.longitude, it.location.latitude),
                properties.toJsonObject()
            )
        }

        // Create window info
        val windowInfoImages = hashMapOf<String, Bitmap>()
        val inflater = LayoutInflater.from(this)
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
            windowInfoImages[id] = bitmap
        }

        setWindowInfoImageGenResults(windowInfoImages)
        deploymentFeatures = FeatureCollection.fromFeatures(pointFeatures)
        refreshSource()

        // set zoom?
        val lastDeployment = deployments.lastOrNull()
        if (lastDeployment != null) {
            moveCamera(LatLng(lastDeployment.location.latitude, lastDeployment.location.longitude))
            setCreateLocationButton(true)
        }
    }

    private fun setWindowInfoImageGenResults(windowInfoImages: HashMap<String, Bitmap>) {
        mapboxMap?.style?.addImages(windowInfoImages)
    }

    private fun setupImages(style: Style) {
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
    }

    private fun setupWindowInfo(it: Style) {
        it.addLayer(SymbolLayer(WINDOW_DEPLOYMENT_ID, SOURCE_DEPLOYMENT).apply {
            withProperties(
                PropertyFactory.iconImage("{$PROPERTY_MARKER_LOCATION_ID}"),
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
                PropertyFactory.iconImage("{$PROPERTY_MARKER_IMAGE}"),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconSize(1f)
            )
        }
        style.addLayer(markerLayer)
    }

    private fun moveCamera(latLng: LatLng) {
        mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0))
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
        private const val IMAGES = "IMAGES"
        private const val DEPLOYMENT_ID = "DEPLOYMENT_ID"

        private const val SOURCE_DEPLOYMENT = "source.deployment"
        private const val MARKER_DEPLOYMENT_ID = "marker.deployment"
        private const val WINDOW_DEPLOYMENT_ID = "info.deployment"

        private const val PROPERTY_SELECTED = "selected"
        private const val PROPERTY_MARKER_LOCATION_ID = "location"
        private const val PROPERTY_MARKER_TITLE = "title"
        private const val PROPERTY_MARKER_CAPTION = "caption"
        private const val PROPERTY_MARKER_IMAGE = "marker-image"

        fun startActivity(
            context: Context,
            images: ArrayList<String>? = null,
            deploymentId: String? = null
        ) {
            val intent = Intent(context, MainActivity::class.java)
            intent.putStringArrayListExtra(IMAGES, images)
            intent.putExtra(DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }
}
