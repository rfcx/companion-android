package org.rfcx.audiomoth.entity.guardian

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.rfcx.audiomoth.entity.DeploymentLocation
import java.util.*

@RealmClass
open class GuardianDeployment(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var deployedAt: Date = Date(),
    @Expose(serialize = false)
    var state: Int = 0, // 1 = Locate, 2 = Config, 3 = Sync, 4 = Verify, 5 = Deploy, 6 = Ready To Upload
    var wifiName: String? = null,
    var configuration: GuardianConfiguration? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date = Date(),
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel {

    companion object {
        const val FIELD_ID = "id"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_STATE = "state"
        const val FIELD_SYNC_STATE = "syncState"
        const val PHOTOS = "photos"
    }
}
