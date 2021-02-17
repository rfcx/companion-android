package org.rfcx.companion.entity

import io.realm.RealmModel
import io.realm.annotations.RealmClass
import java.io.Serializable

@RealmClass
open class DeploymentLocation(
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var project: LocationGroup? = null,
    var coreId: String? = null
) : RealmModel, Serializable {
    companion object {
        const val TABLE_NAME = "DeploymentLocation"
        const val FIELD_PROJECT = "project"
        const val FIELD_ALTITUDE = "altitude"
        const val FIELD_CORE_ID = "coreId"
    }
}
