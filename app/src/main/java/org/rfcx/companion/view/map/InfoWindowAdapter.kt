package org.rfcx.companion.view.map

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import org.rfcx.companion.R

class InfoWindowAdapter(mContext: Context) : GoogleMap.InfoWindowAdapter {
    var mWindow: View = LayoutInflater.from(mContext).inflate(R.layout.layout_map_window_info, null)

    private fun setInfoWindowText(marker: Marker) {
        val title = marker.title
        val data = Gson().fromJson(marker.snippet, MapMarker.DeploymentMarker::class.java)

        val tvTitle = mWindow.findViewById<TextView>(R.id.createdAtValue)
        if (!TextUtils.isEmpty(title)) {
            tvTitle.text = data.deploymentKey
        }
    }

    override fun getInfoWindow(p0: Marker): View {
        setInfoWindowText(p0)
        return mWindow
    }

    override fun getInfoContents(p0: Marker): View {
        setInfoWindowText(p0)
        return mWindow
    }
}