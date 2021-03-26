package org.rfcx.companion.view.map

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.turf.TurfMeasurement

object MapboxCameraUtils {
    fun calculateLatLngForZoom(userPosition: Point, nearestSite: Point? = null, mapboxMap: MapboxMap? = null, zoom: Double): CameraOptions {
        if (nearestSite == null) {
            return CameraOptions.Builder().center(userPosition).zoom(zoom).build()
        }
        val oppositeLat = userPosition.latitude() - (nearestSite.latitude() - userPosition.latitude())
        val oppositeLng = userPosition.longitude() - (nearestSite.longitude() - userPosition.longitude())
        val oppositeNearestSite = Point.fromLngLat(oppositeLng, oppositeLat)
        if (TurfMeasurement.distance(oppositeNearestSite, userPosition) < 0.03) {
            return CameraOptions.Builder().center(userPosition).zoom(zoom).build()
        }
        if (mapboxMap != null) {
            val latLngBounds = CoordinateBounds(oppositeNearestSite, nearestSite)
            return mapboxMap.cameraForCoordinateBounds(latLngBounds, EdgeInsets(100.0, 100.0, 100.0, 100.0), null, null)
        }
        return CameraOptions.Builder().center(userPosition).zoom(zoom).build()
    }
}
