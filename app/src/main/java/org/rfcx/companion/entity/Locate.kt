package org.rfcx.companion.entity

import com.google.gson.annotations.Expose
import com.mapbox.mapboxsdk.geometry.LatLng
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.rfcx.companion.view.map.MapFragment
import org.rfcx.companion.view.map.MapMarker
import java.util.*

@RealmClass
open class Locate(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var locationGroup: LocationGroup? = null,
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var createdAt: Date = Date(),
    var updatedAt: Date? = null,
    var lastDeploymentId: Int = 0,
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel {

    fun getLatLng(): LatLng = LatLng(latitude, longitude)

    fun asDeploymentLocation(): DeploymentLocation {
        return DeploymentLocation(
            coreId = serverId,
            name = name,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            project = locationGroup
        )
    }

    companion object {
        const val TABLE_NAME = "Locate"
        const val FIELD_ID = "id"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_LOCATION_GROUP = "locationGroup"
        const val FIELD_DELETED_AT = "deletedAt"
        const val FIELD_ALTITUDE = "altitude"
        const val FIELD_NAME = "name"
        const val FIELD_LAST_DEPLOYMENT_ID = "lastDeploymentId"
    }
}

fun Locate.toMark(): MapMarker.SiteMarker {
    return MapMarker.SiteMarker(id, name, latitude, longitude, altitude, locationGroup?.name, createdAt, MapFragment.SITE_MARKER)
}
