package org.rfcx.companion.entity

import com.google.gson.annotations.Expose
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
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
    var permissions: String = ""
) : RealmModel {
    companion object {
        const val TABLE_NAME = "LocationGroups"
        const val PROJECT_ID = "id"
        const val PROJECT_NAME = "name"
        const val PROJECT_COLOR = "color"
        const val PROJECT_SERVER_ID = "serverId"
        const val PROJECT_SYNC_STATE = "syncState"
        const val PROJECT_DELETED_AT = "deletedAt"
        const val PROJECT_PERMISSIONS = "permissions"
    }
}

fun Project.toLocationGroup(): LocationGroup {
    return LocationGroup(this.name, this.color, this.serverId)
}
