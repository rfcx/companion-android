package org.rfcx.companion.entity

import io.realm.RealmObject

open class Coordinate : RealmObject() {
    var latitude: Double? = null
    var longitude: Double? = null
    var altitude: Double? = null

    companion object {
        const val TABLE_NAME = "Coordinate"
        const val COORDINATE_LATITUDE = "latitude"
        const val COORDINATE_LONGITUDE = "longitude"
        const val COORDINATE_ALTITUDE = "altitude"
    }
}
