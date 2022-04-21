package org.rfcx.companion.entity

import com.google.gson.annotations.Expose
import com.mapbox.mapboxsdk.geometry.LatLng
import io.realm.RealmModel
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.view.map.MapFragment
import org.rfcx.companion.view.map.MapMarker
import java.util.*

@RealmClass
open class Stream(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var createdAt: Date = Date(),
    var updatedAt: Date? = null,
    var lastDeploymentId: Int = 0,
    @Expose(serialize = false)
    var syncState: Int = 0,
    @LinkingObjects("stream") val deployments: RealmResults<Deployment>? = null,
    var project: Project? = null
) : RealmModel {

    fun getActiveDeployment(): Deployment? {
        return this.deployments?.filter { dp -> dp.isActive }?.getOrNull(0)
    }

    fun getLatLng(): LatLng = LatLng(latitude, longitude)

    companion object {
        const val TABLE_NAME = "Locate"
        const val FIELD_ID = "id"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_PROJECT = "project"
        const val FIELD_DELETED_AT = "deletedAt"
        const val FIELD_ALTITUDE = "altitude"
        const val FIELD_NAME = "name"
        const val FIELD_LAST_DEPLOYMENT_ID = "lastDeploymentId"
    }
}

fun Stream.toMark(): MapMarker.SiteMarker {
    return MapMarker.SiteMarker(id, name, latitude, longitude, altitude, project?.name, createdAt, MapFragment.SITE_MARKER)
}
