package org.rfcx.audiomoth.entity

import com.google.gson.annotations.Expose
import com.mapbox.mapboxsdk.geometry.LatLng
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class Locate(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var createdAt: Date = Date(),
    var lastDeploymentId: Int = 0,
    var lastDeploymentServerId: String? = null,
    var lastGuardianDeploymentId: Int = 0,
    var lastGuardianDeploymentServerId: String? = null,
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel {

    fun getLatLng(): LatLng = LatLng(latitude, longitude)

    fun asDeploymentLocation(): DeploymentLocation {
        return DeploymentLocation(
            name = name,
            latitude = latitude,
            longitude = longitude
        )
    }

    companion object {
        const val FIELD_ID = "id"
    }
}