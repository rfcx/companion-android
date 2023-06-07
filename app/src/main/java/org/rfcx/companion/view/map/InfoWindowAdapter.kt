package org.rfcx.companion.view.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import org.rfcx.companion.R
import org.rfcx.companion.util.latitudeCoordinates
import org.rfcx.companion.util.longitudeCoordinates
import org.rfcx.companion.util.toTimeAgo

class InfoWindowAdapter(var mContext: Context) : GoogleMap.InfoWindowAdapter {
    var mWindow: View = LayoutInflater.from(mContext).inflate(R.layout.layout_map_window_info, null)

    private fun setInfoWindowText(marker: Marker) {
        if (marker.snippet == null) return
        val isDeployment = marker.snippet!!.contains("deploymentKey")

        val data = if (isDeployment) {
            Gson().fromJson(marker.snippet, MapMarker.DeploymentMarker::class.java)
        } else {
            Gson().fromJson(marker.snippet, MapMarker.SiteMarker::class.java)
        }

        when (data) {
            is MapMarker.SiteMarker -> {
                val title = mWindow.findViewById<TextView>(R.id.infoWindowTitle)
                val project = mWindow.findViewById<TextView>(R.id.infoWindowDescription)
                val createdAt = mWindow.findViewById<TextView>(R.id.createdAtValue)
                val latLng = mWindow.findViewById<TextView>(R.id.latLngValue)

                title.text = data.name
                project.text = data.projectName
                createdAt.text = data.createdAt.toTimeAgo(mContext)
                val latLngText = "${data.latitude.latitudeCoordinates(mContext)}, ${
                    data.longitude.longitudeCoordinates(mContext)
                }"
                latLng.text = latLngText
            }

            is MapMarker.DeploymentMarker -> {
                val deploymentSiteTitle = mWindow.findViewById<TextView>(R.id.deploymentSiteTitle)
                val projectName = mWindow.findViewById<TextView>(R.id.projectName)
                val deploymentStreamId = mWindow.findViewById<TextView>(R.id.deploymentStreamId)
                val deploymentTypeName = mWindow.findViewById<TextView>(R.id.deploymentTypeName)
                val deployedAt = mWindow.findViewById<TextView>(R.id.deployedAt)
                val latLngTextView = mWindow.findViewById<TextView>(R.id.latLngTextView)

                deploymentSiteTitle.text = data.locationName
                projectName.text = data.projectName
                deploymentStreamId.visibility = View.VISIBLE
                deploymentStreamId.text = data.deploymentKey
                deploymentTypeName.text = data.device
                deployedAt.text = data.deploymentAt.toTimeAgo(mContext)
                val latLngText = "${data.latitude.latitudeCoordinates(mContext)}, ${
                    data.longitude.longitudeCoordinates(mContext)
                }"
                latLngTextView.text = latLngText
            }
        }
    }

    override fun getInfoWindow(p0: Marker): View {
        if (p0.snippet == null) return mWindow
        if (p0.snippet!!.contains("deploymentKey")) {
            mWindow =
                LayoutInflater.from(mContext).inflate(R.layout.layout_deployment_window_info, null)
        }
        setInfoWindowText(p0)
        return mWindow
    }

    override fun getInfoContents(p0: Marker): View {
        setInfoWindowText(p0)
        return mWindow
    }
}