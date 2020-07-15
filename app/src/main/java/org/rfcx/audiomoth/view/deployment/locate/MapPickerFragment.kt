package org.rfcx.audiomoth.view.deployment.locate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.fragment_map_picker.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.latitudeCoordinates
import org.rfcx.audiomoth.util.longitudeCoordinates

class MapPickerFragment : Fragment(), OnMapReadyCallback {
    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_picker, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { Mapbox.getInstance(it, getString(R.string.mapbox_token)) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapBoxPickerView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        selectButton.setOnClickListener {
            Toast.makeText(context, "SETECT", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(16.0, 100.0), 15.0))
        mapboxMap.setStyle(Style.OUTDOORS)

        mapboxMap.addOnCameraMoveListener {
            val currentCameraPosition = mapboxMap.cameraPosition.target
            setLatLogLabel(LatLng(currentCameraPosition.latitude, currentCameraPosition.longitude))
        }
    }

    private fun setLatLogLabel(location: LatLng) {
        context?.let {
            val latLng =
                "${location.latitude.latitudeCoordinates(it)}, ${location.longitude.longitudeCoordinates(
                    it
                )}"
            locationTextView.text = latLng
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

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    companion object {
        @JvmStatic
        fun newInstance(): MapPickerFragment {
            return MapPickerFragment()
        }
    }
}
