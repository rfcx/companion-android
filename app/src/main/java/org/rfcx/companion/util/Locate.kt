package org.rfcx.companion.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.maps.model.LatLng
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.view.profile.coordinates.CoordinatesActivity.Companion.DDM_FORMAT
import org.rfcx.companion.view.profile.coordinates.CoordinatesActivity.Companion.DD_FORMAT
import org.rfcx.companion.view.profile.coordinates.CoordinatesActivity.Companion.DMS_FORMAT
import kotlin.math.absoluteValue

object DefaultSetupMap {
    const val DEFAULT_ZOOM = 15.0f
}

fun Double?.latitudeCoordinates(context: Context): String {
    val lat = this
    if (lat != null) {
        val directionLatitude = if (lat > 0) "N" else "S"
        var strLatitude = ""

        when (context.getCoordinatesFormat()) {
            DD_FORMAT -> {
                strLatitude = Location.convert(lat.absoluteValue, Location.FORMAT_DEGREES)
                strLatitude = "${replaceDelimitersDD(strLatitude)}$directionLatitude"
            }
            DDM_FORMAT -> {
                strLatitude = Location.convert(lat.absoluteValue, Location.FORMAT_MINUTES)
                strLatitude = "${replaceDelimitersDDM(strLatitude)}$directionLatitude"
            }
            DMS_FORMAT -> {
                strLatitude = Location.convert(lat.absoluteValue, Location.FORMAT_SECONDS)
                strLatitude = "${replaceDelimitersDMS(strLatitude)}$directionLatitude"
            }
        }
        return strLatitude
    }
    return this.toString()
}

fun Double?.longitudeCoordinates(context: Context): String {
    val longitude = this
    if (longitude != null) {
        val directionLongitude = if (longitude > 0) "E" else "W"
        var strLongitude = ""

        when (context.getCoordinatesFormat()) {
            DD_FORMAT -> {
                strLongitude = Location.convert(longitude.absoluteValue, Location.FORMAT_DEGREES)
                strLongitude = "${replaceDelimitersDD(strLongitude)}$directionLongitude"
            }
            DDM_FORMAT -> {
                strLongitude = Location.convert(longitude.absoluteValue, Location.FORMAT_MINUTES)
                strLongitude = "${replaceDelimitersDDM(strLongitude)}$directionLongitude"
            }
            DMS_FORMAT -> {
                strLongitude = Location.convert(longitude.absoluteValue, Location.FORMAT_SECONDS)
                strLongitude = "${replaceDelimitersDMS(strLongitude)}$directionLongitude"
            }
        }
        return strLongitude
    }
    return this.toString()
}

private fun replaceDelimitersDMS(str: String): String {
    var strDMSFormat = str
    strDMSFormat = strDMSFormat.replaceFirst(":".toRegex(), "°")
    strDMSFormat = strDMSFormat.replaceFirst(":".toRegex(), "'")
    val pointIndex = strDMSFormat.indexOf(".")
    val endIndex = pointIndex + 2
    if (endIndex < strDMSFormat.length) {
        strDMSFormat = strDMSFormat.substring(0, endIndex)
    }
    strDMSFormat += "\""
    return strDMSFormat
}

private fun replaceDelimitersDD(str: String): String {
    var strDDFormat = str
    val pointIndex = strDDFormat.indexOf(".")
    val endIndex = pointIndex + 6
    if (endIndex < strDDFormat.length) {
        strDDFormat = strDDFormat.substring(0, endIndex)
    }
    strDDFormat += "°"
    return strDDFormat
}

private fun replaceDelimitersDDM(str: String): String {
    var strDDMFormat = str
    strDDMFormat = strDDMFormat.replaceFirst(":".toRegex(), "°")
    val pointIndex = strDDMFormat.indexOf(".")
    val endIndex = pointIndex + 5
    if (endIndex < strDDMFormat.length) {
        strDDMFormat = strDDMFormat.substring(0, endIndex)
    }
    strDDMFormat += "\'"
    return strDDMFormat
}

fun convertLatLngLabel(context: Context, lat: Double, lng: Double): String {
    return "${lat.latitudeCoordinates(context)}, ${lng.longitudeCoordinates(context)}"
}

fun Location.saveLastLocation(context: Context) {
    val preferences = Preferences.getInstance(context)
    preferences.putFloat(Preferences.LAST_LATITUDE, this.latitude.toFloat())
    preferences.putFloat(Preferences.LAST_LONGITUDE, this.longitude.toFloat())
}

fun LatLng.saveLastLocation(context: Context) {
    val preferences = Preferences.getInstance(context)
    preferences.putFloat(Preferences.LAST_LATITUDE, this.latitude.toFloat())
    preferences.putFloat(Preferences.LAST_LONGITUDE, this.longitude.toFloat())
}

fun Context.getLastLocation(): Location? {
    val preferences = Preferences.getInstance(this)
    val latitude = preferences.getFloat(Preferences.LAST_LATITUDE)
    val longitude = preferences.getFloat(Preferences.LAST_LONGITUDE)
    val lastLocate = Location(LocationManager.GPS_PROVIDER)

    if (latitude == 0.0F && longitude == 0.0F) return null

    lastLocate.latitude = latitude.toDouble()
    lastLocate.longitude = longitude.toDouble()

    return lastLocate
}

fun Location.toLatLng(): LatLng {
    return LatLng(this.latitude, this.longitude)
}

fun Stream.toLatLng(): LatLng {
    return LatLng(this.latitude, this.longitude)
}
