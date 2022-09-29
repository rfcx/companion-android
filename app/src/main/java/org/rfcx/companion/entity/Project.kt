package org.rfcx.companion.entity

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.io.Serializable
import java.util.*

@RealmClass
open class Project(
    @PrimaryKey
    var id: Int = 0,
    var name: String? = null,
    var color: String? = null,
    var serverId: String? = null,
    @Expose(serialize = false)
    var syncState: Int = 0,
    var deletedAt: Date? = null,
    var permissions: String = "",
    var minLatitude: Double? = null,
    var minLongitude: Double? = null,
    var maxLatitude: Double? = null,
    var maxLongitude: Double? = null,
    var offlineMapState: String = OfflineMapState.DOWNLOAD_STATE.key,
    var offTimes: String? = null,
    @LinkingObjects("project") val streams: RealmResults<Stream>? = null
) : RealmModel, Serializable {
    companion object {
        const val TABLE_NAME = "Project"
        const val PROJECT_ID = "id"
        const val PROJECT_NAME = "name"
        const val PROJECT_COLOR = "color"
        const val PROJECT_SERVER_ID = "serverId"
        const val PROJECT_SYNC_STATE = "syncState"
        const val PROJECT_DELETED_AT = "deletedAt"
        const val PROJECT_PERMISSIONS = "permissions"
        const val PROJECT_MIN_LATITUDE = "minLatitude"
        const val PROJECT_MIN_LONGITUDE = "minLongitude"
        const val PROJECT_MAX_LATITUDE = "maxLatitude"
        const val PROJECT_MAX_LONGITUDE = "maxLongitude"
        const val PROJECT_OFFLINE_MAP_STATE = "offlineMapState"
        const val PROJECT_OFF_TIMES = "offTimes"
    }
}

fun Project.isGuest(): Boolean {
    return this.permissions == Permissions.GUEST.value
}
