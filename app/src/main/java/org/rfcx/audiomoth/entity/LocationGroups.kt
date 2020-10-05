package org.rfcx.audiomoth.entity

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class LocationGroups(
    @PrimaryKey
    var id: Int = 0,
    var name: String = "",
    var color: String = "",
    @Expose(serialize = false)
    var syncState: Int = 0

) : RealmModel {
    companion object {
        const val TABLE_NAME = "LocationGroups"
        const val LOCATION_GROUPS_ID = "id"
        const val LOCATION_GROUPS_NAME = "name"
        const val LOCATION_GROUPS_COLOR = "color"
        const val LOCATION_GROUPS_SYNC_STATE = "syncState"
    }
}
