package org.rfcx.companion.view.deployment.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import org.rfcx.companion.R

class DetailDeploymentSiteFragment : Fragment(), OnMapReadyCallback {

    // Mapbox
    private var mapboxMap: MapboxMap? = null
    private lateinit var mapView: MapView

    // Arguments
    var siteId: Int = 0
    var siteName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { Mapbox.getInstance(it, getString(R.string.mapbox_token)) }
        initIntent()
    }

    private fun initIntent() {
        arguments?.let {
            siteId = it.getInt(ARG_SITE_ID)
            siteName = it.getString(ARG_SITE_NAME) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_detail_deployment_site, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapBoxView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.uiSettings.setAllGesturesEnabled(false)
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false
        mapboxMap.setStyle(Style.OUTDOORS) { }
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
        private const val ARG_SITE_ID = "ARG_SITE_ID"
        private const val ARG_SITE_NAME = "ARG_SITE_NAME"

        @JvmStatic
        fun newInstance(id: Int, name: String?) =
            DetailDeploymentSiteFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SITE_ID, id)
                    putString(ARG_SITE_NAME, name)
                }
            }
    }
}
