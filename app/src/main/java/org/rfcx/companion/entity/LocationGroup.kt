package org.rfcx.companion.entity

import io.realm.RealmModel
import io.realm.annotations.RealmClass
import java.io.Serializable

@RealmClass
open class LocationGroup(
    var name: String? = null,
    var color: String? = null,
    var coreId: String? = null
) : RealmModel, Serializable {
    companion object {
        const val TABLE_NAME = "LocationGroup"
        const val FIELD_NAME = "name"
        const val FIELD_COLOR = "color"
        const val FIELD_CORE_ID = "coreId"
    }
}
