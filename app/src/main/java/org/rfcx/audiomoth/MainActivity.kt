package org.rfcx.audiomoth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import kotlinx.android.synthetic.main.activity_main.*
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.view.configure.ConfigureActivity
import org.rfcx.audiomoth.view.configure.LocationFragment.Companion.MAPBOX_ACCESS_TOKEN

open class MainActivity : AppCompatActivity() {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapView: MapView
    private lateinit var symbolManager: SymbolManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)

        inputDeviceIdButton.setOnClickListener {
            ConfigureActivity.startActivity(this)
            finish()
        }

        mapView = findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            mapboxMap.setStyle(Style.OUTDOORS) {
                setUpImage(it)
                symbolManager = SymbolManager(mapView, mapboxMap, it)
                symbolManager.iconAllowOverlap = true
                symbolManager.iconIgnorePlacement = true
                enableLocationComponent(it)
                getDevices()
            }
        }
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        val locationComponent = mapboxMap.locationComponent
        locationComponent.activateLocationComponent(this, loadedMapStyle)
        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.COMPASS
    }

    private fun getDevices() {
        val docRef = Firestore().db.collection(USERS)
        docRef.whereEqualTo("name", "Ratree Onchana").get()
            .addOnSuccessListener { documents ->
                symbolManager.deleteAll()
                for (document in documents) {
                    docRef.document(document.id)
                        .collection(DEPLOYMENTS).get()
                        .addOnSuccessListener { subDocuments ->
                            for (sub in subDocuments) {
                                val isLatest = sub.data["isLatest"] as Boolean
                                if (isLatest) {
                                    val location = sub.data["location"] as Map<*, *>
                                    val latitude = location["latitude"] as Double
                                    val longitude = location["longitude"] as Double
                                    val batteryPredicted =
                                        sub.data["batteryPredictedAt"] as com.google.firebase.Timestamp

                                    displayPinOfDevices(
                                        LatLng(latitude, longitude),
                                        checkBatteryPredictedUntil(batteryPredicted.seconds * 1000)
                                    )

                                    moveCamera(LatLng(latitude, longitude))
                                }
                            }
                        }
                }
            }
    }

    private fun setUpImage(style: Style) {
        val drawablePinMapGreen =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map, null)
        val mBitmapPinMapGreen = BitmapUtils.getBitmapFromDrawable(drawablePinMapGreen)
        if (mBitmapPinMapGreen != null) {
            style.addImage(PIN_MAP_GREEN, mBitmapPinMapGreen)
        }

        val drawablePinMapOrange =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map_orange, null)
        val mBitmapPinMapOrange = BitmapUtils.getBitmapFromDrawable(drawablePinMapOrange)
        if (mBitmapPinMapOrange != null) {
            style.addImage(PIN_MAP_ORANGE, mBitmapPinMapOrange)
        }

        val drawablePinMapRed =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map_red, null)
        val mBitmapPinMapRed = BitmapUtils.getBitmapFromDrawable(drawablePinMapRed)
        if (mBitmapPinMapRed != null) {
            style.addImage(PIN_MAP_RED, mBitmapPinMapRed)
        }
    }

    private fun checkBatteryPredictedUntil(timestamp: Long): String {
        val currentMillis = System.currentTimeMillis()
        val threeDays = 3 * 24 * 60 * 60 * 1000
        val oneDay = 24 * 60 * 60 * 1000

        return if (timestamp > (currentMillis + threeDays)) {
            PIN_MAP_GREEN
        } else if (timestamp > (currentMillis + oneDay) && timestamp < (currentMillis + threeDays)) {
            PIN_MAP_ORANGE
        } else {
            PIN_MAP_RED
        }
    }

    private fun displayPinOfDevices(latLng: LatLng, imageName: String) {
        symbolManager.create(
            SymbolOptions()
                .withLatLng(latLng)
                .withIconImage(imageName)
                .withIconSize(1.0f)
        )
    }

    private fun moveCamera(latLng: LatLng) {
        mapboxMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng,
                15.0
            )
        )
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
        const val TAG = "MainActivity"
        const val PIN_MAP_GREEN = "PIN_MAP_GREEN"
        const val PIN_MAP_ORANGE = "PIN_MAP_ORANGE"
        const val PIN_MAP_RED = "PIN_MAP_RED"
        const val SHOW_SNACKBAR = "SHOW_SNACKBAR"

        const val USERS = "users"
        const val DEPLOYMENTS = "deployments"
        const val LOCATIONS = "locations"
    }
}
