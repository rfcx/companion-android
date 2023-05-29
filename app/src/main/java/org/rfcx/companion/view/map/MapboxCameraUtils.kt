package org.rfcx.companion.view.map

//import com.mapbox.mapboxsdk.camera.CameraUpdate
//import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
//import com.mapbox.mapboxsdk.geometry.LatLng
//import com.mapbox.mapboxsdk.geometry.LatLngBounds

object MapboxCameraUtils {
//    fun calculateLatLngForZoom(userPosition: LatLng, nearestSite: LatLng? = null, zoom: Double): CameraUpdate {
//        if (nearestSite == null) {
//            return CameraUpdateFactory.newLatLngZoom(userPosition, zoom)
//        }
//        val oppositeLat = userPosition.latitude - (nearestSite.latitude - userPosition.latitude)
//        val oppositeLng = userPosition.longitude - (nearestSite.longitude - userPosition.longitude)
//        if (oppositeLat >= -90 && oppositeLat <= 90 && oppositeLng >= -180 && oppositeLng <= 180) {
//            val oppositeNearestSite = LatLng(oppositeLat, oppositeLng)
//            if (oppositeNearestSite.distanceTo(userPosition) < 30) {
//                return CameraUpdateFactory.newLatLngZoom(userPosition, zoom)
//            }
//            val latLngBounds = LatLngBounds.Builder()
//                .include(oppositeNearestSite)
//                .include(nearestSite)
//                .build()
//            return CameraUpdateFactory.newLatLngBounds(latLngBounds, 100)
//        }
//        return CameraUpdateFactory.newLatLngZoom(userPosition, zoom)
//    }
}
