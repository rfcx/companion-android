package org.rfcx.companion.entity.guardian

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.io.Serializable
import java.util.*
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.Device

@RealmClass
open class GuardianDeployment(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var deployedAt: Date = Date(),
    @Expose(serialize = false)
    var state: Int = 0, // 1 = Locate, 2 = Config, 3 = Sync, 4 = Verify, 5 = Deploy, 6 = Ready To Upload
    var device: String? = Device.GUARDIAN.value,
    var wifiName: String? = "",
    var configuration: GuardianConfiguration? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date = Date(),
    var updatedAt: Date? = null,
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel, Serializable {

    companion object {
        const val TABLE_NAME = "GuardianDeployment"
        const val FIELD_ID = "id"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_STATE = "state"
        const val FIELD_SYNC_STATE = "syncState"
        const val FIELD_LOCATION = "location"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}