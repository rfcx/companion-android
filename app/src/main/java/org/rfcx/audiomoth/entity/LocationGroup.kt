package org.rfcx.audiomoth.entity

import io.realm.RealmModel
import io.realm.annotations.RealmClass
import java.io.Serializable

@RealmClass
open class LocationGroup(
    var group: String? = null,
    var color: String? = null,
    var serverId: String? = null
) : RealmModel {
    companion object {
        const val TABLE_NAME = "LocationGroup"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_GROUP = "group"
        const val FIELD_COLOR = "color"
    }
}
