package org.rfcx.companion.entity

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class LocationGroups(
    @PrimaryKey
    var id: Int = 0,
    var name: String? = null,
    var color: String? = null,
    var serverId: String? = null,
    @Expose(serialize = false)
    var syncState: Int = 0,
    var deletedAt: Date? = null
) : RealmModel {
    companion object {
        const val TABLE_NAME = "LocationGroups"
        const val LOCATION_GROUPS_ID = "id"
        const val LOCATION_GROUPS_NAME = "name"
        const val LOCATION_GROUPS_COLOR = "color"
        const val LOCATION_GROUPS_SERVER_ID = "serverId"
        const val LOCATION_GROUPS_SYNC_STATE = "syncState"
        const val LOCATION_GROUPS_DELETE_AT = "deletedAt"
    }
}

fun LocationGroups.toLocationGroup(): LocationGroup {
    return LocationGroup(this.name, this.color, this.serverId)
}
