package org.rfcx.companion.view.map

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil

object MapboxCameraUtils {
    fun calculateLatLngForZoom(userPosition: LatLng, nearestSite: LatLng? = null, zoom: Float): CameraUpdate {
        if (nearestSite == null) {
            return CameraUpdateFactory.newLatLngZoom(userPosition, zoom)
        }
        val oppositeLat = userPosition.latitude - (nearestSite.latitude - userPosition.latitude)
        val oppositeLng = userPosition.longitude - (nearestSite.longitude - userPosition.longitude)
        if (oppositeLat >= -90 && oppositeLat <= 90 && oppositeLng >= -180 && oppositeLng <= 180) {
            val oppositeNearestSite = LatLng(oppositeLat, oppositeLng)
            val distance = SphericalUtil.computeDistanceBetween(oppositeNearestSite, userPosition);
            if (distance < 30) {
                return CameraUpdateFactory.newLatLngZoom(userPosition, zoom)
            }
            val latLngBounds = LatLngBounds.Builder()
                .include(oppositeNearestSite)
                .include(nearestSite)
                .build()
            return CameraUpdateFactory.newLatLngBounds(latLngBounds, 100)
        }
        return CameraUpdateFactory.newLatLngZoom(userPosition, zoom)
    }
}
