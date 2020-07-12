package org.rfcx.audiomoth.entity

import io.realm.RealmModel
import io.realm.annotations.RealmClass
import java.io.Serializable

@RealmClass
open class DeploymentLocation(
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
) : RealmModel, Serializable {
    companion object {
        fun default() = DeploymentLocation(
            name = "",
            latitude = 0.0,
            longitude = 0.0
        )
    }
}
