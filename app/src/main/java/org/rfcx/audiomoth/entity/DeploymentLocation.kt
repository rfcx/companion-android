package org.rfcx.audiomoth.entity

import io.realm.RealmModel
import io.realm.annotations.RealmClass
import java.io.Serializable

@RealmClass
open class DeploymentLocation(
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var locationGroup: LocationGroup? = null
) : RealmModel, Serializable {
    companion object {
        const val TABLE_NAME = "DeploymentLocation"
        const val FIELD_LOCATION_GROUP = "locationGroup"
    }
}
