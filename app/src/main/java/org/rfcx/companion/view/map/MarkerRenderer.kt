package org.rfcx.companion.view.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import org.rfcx.companion.R
import org.rfcx.companion.entity.MarkerItem
import org.rfcx.companion.util.Pin

class MarkerRenderer(
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<MarkerItem>
) : DefaultClusterRenderer<MarkerItem>(context, map, clusterManager) {

    /**
     * Method called before the cluster item (the marker) is rendered.
     * This is where marker options should be set.
     */
    override fun onBeforeClusterItemRendered(
        item: MarkerItem,
        markerOptions: MarkerOptions
    ) {
        val isDeployment = item.snippet.contains("deploymentKey")
        var pin = R.drawable.ic_pin_map_grey
        if (isDeployment) {
            val data = Gson().fromJson(item.snippet, MapMarker.DeploymentMarker::class.java)
            pin = if (data.pin == Pin.PIN_GREEN) {
                R.drawable.ic_pin_map
            } else {
                R.drawable.ic_pin_map_grey
            }
        }

        markerOptions.title(item.title)
            .position(item.position)
            .snippet(item.snippet)
            .icon(bitmapFromVector(context, pin))
    }

    override fun getColor(clusterSize: Int): Int {
        return Color.parseColor("#2AA841")
    }

    private fun bitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        //drawable generator
        val vectorDrawable: Drawable = ContextCompat.getDrawable(context, vectorResId)!!
        vectorDrawable.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        //bitmap genarator
        val bitmap: Bitmap =
            Bitmap.createBitmap(
                vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        //canvas genaret
        //pass bitmap in canvas constructor
        val canvas: Canvas = Canvas(bitmap)
        //pass canvas in drawable
        vectorDrawable.draw(canvas)
        //return BitmapDescriptorFactory
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    /**
     * Method called right after the cluster item (the marker) is rendered.
     * This is where properties for the Marker object should be set.
     */
    override fun onClusterItemRendered(clusterItem: MarkerItem, marker: Marker) {
        marker.tag = clusterItem
    }
}