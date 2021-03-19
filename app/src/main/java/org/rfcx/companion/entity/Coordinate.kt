package org.rfcx.companion.entity

import io.realm.RealmObject

open class Coordinate(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0
) : RealmObject() {
    companion object {
        const val TABLE_NAME = "Coordinate"
        const val COORDINATE_LATITUDE = "latitude"
        const val COORDINATE_LONGITUDE = "longitude"
        const val COORDINATE_ALTITUDE = "altitude"
    }
}

fun Coordinate.toDoubleArray(): DoubleArray {
    return listOf(this.latitude, this.longitude, this.altitude).toDoubleArray()
}
