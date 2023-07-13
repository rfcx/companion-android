package org.rfcx.companion.view.map

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import org.rfcx.companion.R
import org.rfcx.companion.entity.InfoWindowMarker
import org.rfcx.companion.util.latitudeCoordinates
import org.rfcx.companion.util.longitudeCoordinates
import org.rfcx.companion.util.toTimeAgo

class InfoWindowAdapter(var mContext: Context) : GoogleMap.InfoWindowAdapter {
    var mWindow: View = LayoutInflater.from(mContext).inflate(R.layout.layout_deployment_window_info, null)

    @SuppressLint("SetTextI18n")
    private fun setInfoWindowText(marker: Marker) {
        if (marker.snippet == null) return
        val data = Gson().fromJson(marker.snippet, InfoWindowMarker::class.java)

        val deploymentSiteTitle = mWindow.findViewById<TextView>(R.id.deploymentSiteTitle)
        val projectName = mWindow.findViewById<TextView>(R.id.projectName)
        val deploymentStreamId = mWindow.findViewById<TextView>(R.id.deploymentStreamId)
        val deploymentTypeName = mWindow.findViewById<TextView>(R.id.deploymentTypeName)
        val dateAt = mWindow.findViewById<TextView>(R.id.dateAt)
        val latLngTextView = mWindow.findViewById<TextView>(R.id.latLngTextView)
        val seeDeploymentDetail = mWindow.findViewById<TextView>(R.id.seeDeploymentDetail)

        deploymentStreamId.visibility = if (data.isDeployment) View.VISIBLE else View.GONE
        deploymentTypeName.visibility = if (data.isDeployment) View.VISIBLE else View.GONE
        seeDeploymentDetail.visibility = if (data.isDeployment) View.VISIBLE else View.GONE

        deploymentSiteTitle.text = data.locationName
        projectName.text = data.projectName
        deploymentStreamId.text = mContext.getString(R.string.id_title) + data.deploymentKey
        deploymentTypeName.text = mContext.getString(R.string.type_title) + data.device?.toUpperCase()
        if (data.isDeployment) {
            dateAt.text = "${mContext.getString(R.string.deployed_at)} ${data.deploymentAt?.toTimeAgo(mContext)}"
        } else {
            dateAt.text = "${mContext.getString(R.string.created_at)} ${data.createdAt.toTimeAgo(mContext)}"
        }
        val latLngText = "${data.latitude.latitudeCoordinates(mContext)}, ${
            data.longitude.longitudeCoordinates(mContext)
        }"
        latLngTextView.text = latLngText
    }

    override fun getInfoWindow(p0: Marker): View {
        if (p0.snippet == null) return mWindow
        setInfoWindowText(p0)
        return mWindow
    }

    override fun getInfoContents(p0: Marker): View {
        setInfoWindowText(p0)
        return mWindow
    }
}