package org.rfcx.companion.entity

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
    var locationGroup: LocationGroup? = null,
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var createdAt: Date = Date(),
    var deletedAt: Date? = null,
    var lastDeploymentId: Int = 0,
    var lastDeploymentServerId: String? = null,
    var lastGuardianDeploymentId: Int = 0,
    var lastGuardianDeploymentServerId: String? = null,
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel {

    fun isCompleted(): Boolean {
        return (lastDeploymentId != 0 || lastDeploymentServerId != null || lastGuardianDeploymentId != 0 || lastGuardianDeploymentServerId != null) && deletedAt == null
    }

    fun getLastDeploymentId(): String {
        // is sent?
        return if (lastDeploymentServerId != null || lastGuardianDeploymentServerId != null) {
            // is last deployment from edge?
            if (lastDeploymentServerId != null) {
                lastDeploymentServerId!!
            } else {
                lastGuardianDeploymentServerId!!
            }
        } else {
            if (lastDeploymentId != 0) {
                lastDeploymentId
            } else {
                lastGuardianDeploymentId
            }.toString()
        }
    }

    fun getLatLng(): LatLng = LatLng(latitude, longitude)

    fun asDeploymentLocation(): DeploymentLocation {
        return DeploymentLocation(
            name = name,
            latitude = latitude,
            longitude = longitude,
            locationGroup = locationGroup
        )
    }

    companion object {
        const val TABLE_NAME = "Locate"
        const val FIELD_ID = "id"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_LOCATION_GROUP = "locationGroup"
        const val FIELD_DELETED_AT = "deletedAt"
        const val FIELD_LAST_EDGE_DEPLOYMENT_SERVER_ID = "lastDeploymentServerId"
        const val FIELD_LAST_EDGE_DEPLOYMENT_ID = "lastDeploymentId"
    }
}