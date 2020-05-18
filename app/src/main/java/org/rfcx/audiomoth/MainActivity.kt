package org.rfcx.audiomoth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import kotlinx.android.synthetic.main.activity_main.*
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.util.Battery
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.util.FirestoreResponseCallback
import org.rfcx.audiomoth.view.DeploymentActivity

open class MainActivity : AppCompatActivity() {
    private var mapboxMap: MapboxMap? = null
    private lateinit var symbolManager: SymbolManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_token))
        setContentView(R.layout.activity_main)

        setView(savedInstanceState)
    }

    private fun setView(savedInstanceState: Bundle?) {
        setCreateLocationButton(false)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            mapboxMap.setStyle(Style.OUTDOORS) {
                setUpImage(it)
                symbolManager = SymbolManager(mapView, mapboxMap, it)
                symbolManager.iconAllowOverlap = true
                symbolManager.iconIgnorePlacement = true
                enableLocationComponent(it)
                getLocation()
            }
        }

        createLocationButton.setOnClickListener {
            DeploymentActivity.startActivity(this)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setCreateLocationButton(show: Boolean) {
        createLocationButton.visibility = if (show) View.VISIBLE else View.GONE
        progressBar.visibility = if (!show) View.VISIBLE else View.GONE
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        val locationComponent = mapboxMap?.locationComponent
        locationComponent?.activateLocationComponent(this, loadedMapStyle)
        locationComponent?.isLocationComponentEnabled = true
        locationComponent?.cameraMode = CameraMode.TRACKING
        locationComponent?.renderMode = RenderMode.COMPASS
    }

    private fun getLocation() {
        Firestore().getDeployments(object : FirestoreResponseCallback<List<Deployment?>> {
            override fun onSuccessListener(response: List<Deployment?>) {
                response.forEach {
                    if (it != null) {
                        displayPinOfDevices(
                            LatLng(it.location.latitude, it.location.longitude),
                            Battery.getBatteryPinImage(it.batteryDepletedAt.time)
                        )

                        moveCamera(LatLng(it.location.latitude, it.location.longitude))
                        setCreateLocationButton(true)
                    }
                }
            }

            override fun addOnFailureListener(exception: Exception) {
                setCreateLocationButton(true)
            }
        })
    }

    private fun setUpImage(style: Style) {
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

    private fun displayPinOfDevices(latLng: LatLng, imageName: String) {
        symbolManager.create(
            SymbolOptions()
                .withLatLng(latLng)
                .withIconImage(imageName)
                .withIconSize(1.0f)
        )
    }

    private fun moveCamera(latLng: LatLng) {
        mapboxMap?.moveCamera(
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
}
