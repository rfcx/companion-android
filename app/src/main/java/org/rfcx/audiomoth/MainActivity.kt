package org.rfcx.audiomoth

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.mapboxsdk.annotations.BubbleLayout
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_bottom_navigation_menu.*
import kotlinx.android.synthetic.main.layout_map_window_info.view.*
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.util.*
import org.rfcx.audiomoth.view.DeploymentActivity
import org.rfcx.audiomoth.view.configure.MapFragment
import org.rfcx.audiomoth.view.configure.ProfileFragment
import org.rfcx.audiomoth.widget.BottomNavigationMenuItem

open class MainActivity : AppCompatActivity() {

    private var currentFragment: Fragment? = null

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
        setContentView(R.layout.activity_main)

        createLocationButton.setOnClickListener {
            DeploymentActivity.startActivity(this)
        }

        setupBottomMenu()
        if (savedInstanceState == null) {
            setupFragments()
        }

//        logoutImageView.setOnClickListener {
//            Preferences.getInstance(this).clear()
//            LoginActivity.startActivity(this)
//            finish()
//        }
        setSyncImage()
        progressBar.visibility = View.GONE
        logoutImageView.visibility = View.GONE
    }

    private fun setupBottomMenu() {
        menuMap.setOnClickListener {
            onBottomMenuClick(it)
        }

        menuProfile.setOnClickListener {
            onBottomMenuClick(it)
        }

        menuMap.performClick()
    }

    private fun onBottomMenuClick(menu: View) {
        if ((menu as BottomNavigationMenuItem).menuSelected) return
        when (menu.id) {
            menuMap.id -> {
                menuMap.menuSelected = true
                menuProfile.menuSelected = false

                showMap()
            }

            menuProfile.id -> {
                menuMap.menuSelected = false
                menuProfile.menuSelected = true

                showProfile()
            }
        }
    }

    private fun showProfile() {
        showAboveAppbar(true)
        this.currentFragment = getProfile()
        supportFragmentManager.beginTransaction()
            .show(getProfile())
            .hide(getMap())
            .commit()
    }

    private fun showMap() {
        showAboveAppbar(false)
        this.currentFragment = getMap()
        supportFragmentManager.beginTransaction()
            .show(getMap())
            .hide(getProfile())
            .commit()
    }

    private fun showAboveAppbar(show: Boolean) {
        val contentContainerPaddingBottom =
            if (show) resources.getDimensionPixelSize(R.dimen.button_battery_lv) else 0
        contentContainer.setPadding(0, 0, 0, contentContainerPaddingBottom)
    }

    private fun getMap(): MapFragment =
        supportFragmentManager.findFragmentByTag(MapFragment.tag) as MapFragment?
            ?: MapFragment.newInstance()

    private fun getProfile(): ProfileFragment =
        supportFragmentManager.findFragmentByTag(ProfileFragment.tag) as ProfileFragment?
            ?: ProfileFragment.newInstance()

    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .add(contentContainer.id, getProfile(), ProfileFragment.tag)
            .add(contentContainer.id, getMap(), MapFragment.tag)
            .commit()

        menuMap.performClick()
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

//    override fun onMapReady(mapboxMap: MapboxMap) {
//        this.mapboxMap = mapboxMap
//        mapboxMap.setStyle(Style.OUTDOORS) {
//            checkThenAccquireLocation(it)
//            setupSources(it)
//            setupImages(it)
//            setupMarkerLayers(it)
//            setupWindowInfo(it)
//
//            getDeployments()
//            mapboxMap.addOnMapClickListener { latLng ->
//                handleClickIcon(mapboxMap.projection.toScreenLocation(latLng))
//            }
//        }
//    }

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


    private fun setCreateLocationButton(show: Boolean) { // delete
//        progressBar.visibility = if (!show) View.VISIBLE else View.GONE
    }

    private fun getDeployments() {
        Firestore(this).getDeployments(object : FirestoreResponseCallback<List<Deployment>> {
            override fun onSuccessListener(response: List<Deployment>) {
                setCreateLocationButton(true)
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
        }
    }

    private fun setWindowInfoImageGenResults(windowInfoImages: HashMap<String, Bitmap>) {
//        mapboxMap?.style?.addImages(windowInfoImages)
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
//        mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0))
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
