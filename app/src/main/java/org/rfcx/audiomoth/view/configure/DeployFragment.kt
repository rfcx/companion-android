package org.rfcx.audiomoth.view.configure


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils
import org.rfcx.audiomoth.R

class DeployFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapView: MapView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deploy, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { Mapbox.getInstance(it, MAPBOX_ACCESS_TOKEN) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.OUTDOORS) {
            val symbolManager = SymbolManager(mapView, mapboxMap, it)
            symbolManager.iconAllowOverlap = true
            symbolManager.iconIgnorePlacement = true

            val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_pin_map, null)
            val mBitmap = BitmapUtils.getBitmapFromDrawable(drawable)
            if (mBitmap != null) {
                it.addImage(PIN_MAP, mBitmap)
            }

            symbolManager.create(
                SymbolOptions()
                    .withLatLng(LatLng(-2.4896794, -46.43152714))
                    .withIconImage(PIN_MAP)
                    .withIconSize(1.0f)
            )

            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-2.4896794, -46.43152714), 15.0))
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
        const val PIN_MAP = "pin-map"
        const val MAPBOX_ACCESS_TOKEN =
            "pk.eyJ1IjoicmF0cmVlLW9jaG4iLCJhIjoiY2s5Mjk5MDQ3MDYzcDNmbzVnZHd1aXNqaSJ9.UCrMjgGw8zROm_sRlebSGQ"
    }
}
