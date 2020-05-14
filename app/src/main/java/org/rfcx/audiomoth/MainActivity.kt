package org.rfcx.audiomoth

import android.content.Context
import android.content.Intent
import android.graphics.Paint.UNDERLINE_TEXT_FLAG
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_input_deviec_id_bottom_sheet.*
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICES
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

                getDevices()
            }
        }
    }

    private fun getDevices() {
        var lastProfile: Long = 0
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
                            val timestamp =
                                it.data?.get("batteryPredictedUntil") as com.google.firebase.Timestamp
                            val timestampDeployed =
                                it.data?.get("deployedAt") as com.google.firebase.Timestamp
                            if (lastProfile < timestampDeployed.seconds) {
                                lastProfile = timestampDeployed.seconds
                                moveCamera(LatLng(latitude, longitude))
                            }
                            displayPinOfDevices(
                                LatLng(latitude, longitude),
                                checkBatteryPredictedUntil(timestamp.seconds * 1000)
                            )
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

    private fun Snackbar.allowInfiniteLines(): Snackbar {
        return apply {
            (view.findViewById<View?>(R.id.snackbar_text) as? TextView?)?.isSingleLine = false
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val PIN_MAP_GREEN = "PIN_MAP_GREEN"
        const val PIN_MAP_ORANGE = "PIN_MAP_ORANGE"
        const val PIN_MAP_RED = "PIN_MAP_RED"
        const val SHOW_SNACKBAR = "SHOW_SNACKBAR"
        fun startActivity(context: Context, showSnackbar: Boolean = false) {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(SHOW_SNACKBAR, showSnackbar)
            context.startActivity(intent)
        }
    }
}

interface InputDeviceIdListener {
    fun onSelectedScanQrCode()
    fun onSelectedEnterDeviceId()
}

class InputDeviceIdBottomSheet(private val listener: InputDeviceIdListener) :
    BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_input_deviec_id_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterCodeTextView.paintFlags = enterCodeTextView.paintFlags or UNDERLINE_TEXT_FLAG
        setView()
    }

    private fun setView() {
        enterCodeTextView.setOnClickListener { listener.onSelectedEnterDeviceId() }
        scanQrCodeButton.setOnClickListener { listener.onSelectedScanQrCode() }
    }

    companion object {
        const val TAG = "InputDeviceIdBottomSheet"
    }
}
