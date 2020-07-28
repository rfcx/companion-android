package org.rfcx.audiomoth.entity

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class Deployment(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var batteryDepletedAt: Date = Date(),
    var deployedAt: Date = Date(),
    var deploymentId: String? = null, // random when edge
    var batteryLevel: Int = 0,
    @Expose(serialize = false)
    var state: Int = 0, // 1 = Locate, 2 = Config, 3 = Sync, 4 = Verify, 5 = Deploy, 6 = Ready To Upload
    var configuration: Configuration? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date = Date(),
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel {

    companion object {
        const val TABLE_NAME = "Deployment"
        const val FIELD_ID = "id"
        const val FIELD_STATE = "state"
        const val FIELD_SYNC_STATE = "syncState"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_DEPLOYMENT_ID = "deploymentId"

        const val PHOTOS = "photos"
    }
}
