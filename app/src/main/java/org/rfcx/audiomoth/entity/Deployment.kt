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
    var batteryDepletedAt: Date = Date(),
    var deployedAt: Date = Date(),
    var batteryLevel: Int = 0,
    var state: Int = 0, // 1 = Locate, 2 = Config, 3 = Sync, 4 = Verify, 5 = Deploy, 6 = Ready To Upload
    var configuration: Configuration? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date = Date(),
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel {

    companion object {
        const val FIELD_ID = "id"

        const val LAST_DEPLOYMENT = "lastDeployment"
        const val PHOTOS = "photos"
        const val IS_LATEST = "latest"
        const val DEPLOYED_AT = "deployedAt"
    }
}