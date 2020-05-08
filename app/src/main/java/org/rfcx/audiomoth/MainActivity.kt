package org.rfcx.audiomoth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.view.CreateStreamActivity
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICES
import org.rfcx.audiomoth.view.configure.DeployFragment

open class MainActivity : AppCompatActivity() {
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapView: MapView
    private lateinit var symbolManager: SymbolManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, DeployFragment.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            mapboxMap.setStyle(Style.OUTDOORS) {
                setUpImage(it)
                symbolManager = SymbolManager(mapView, mapboxMap, it)
                symbolManager.iconAllowOverlap = true
                symbolManager.iconIgnorePlacement = true

                getDevices()
            }
        }
    }

    private fun getDevices() {
        val docRef = Firestore().db.collection(DEVICES)
        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null) {
                    val data = documentSnapshot.documents
                    symbolManager.deleteAll()

                    data.map {
                        if (it.data != null) {
                            val location = it.data?.get("location") as Map<*, *>
                            val latitude = location["lat"] as Double
                            val longitude = location["lng"] as Double

                            displayPinOfDevices(LatLng(latitude, longitude))

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

    private fun displayPinOfDevices(latLng: LatLng) {
        symbolManager.create(
            SymbolOptions()
                .withLatLng(latLng)
                .withIconImage(PIN_MAP_RED)
                .withIconSize(1.0f)
        )

        mapboxMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng,
                15.0
            )
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, getText(R.string.code_empty), Toast.LENGTH_SHORT).show()
            } else {
                CreateStreamActivity.startActivity(this, result.contents.split("=")[1])
                finish()
            }

        } else {
            // the camera will not close if the result is still null
            super.onActivityResult(requestCode, resultCode, data)
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
        const val TAG = "MainActivity"
        const val PIN_MAP_GREEN = "PIN_MAP_GREEN"
        const val PIN_MAP_ORANGE = "PIN_MAP_ORANGE"
        const val PIN_MAP_RED = "PIN_MAP_RED"
        fun startActivity(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}
