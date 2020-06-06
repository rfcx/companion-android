package org.rfcx.audiomoth.entity

import com.google.gson.annotations.Expose
import com.mapbox.mapboxsdk.geometry.LatLng
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class DeploymentLocation(
    @PrimaryKey
    var id: Int = 0,
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    @Expose(serialize = false)
    var syncState: Int = 0,
    var createdAt: Date = Date()
) : RealmModel {
    companion object {
        const val FIELD_ID = "id"

        fun default() = DeploymentLocation(
            name = "",
            latitude = 0.0,
            longitude = 0.0
        )
    }
}

data class LocateItem(val id: Int, val name: String, val latitude: Double, val longitude: Double) {
    fun getLatLng(): LatLng = LatLng(latitude, longitude)
}