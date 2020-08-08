package org.rfcx.audiomoth.entity.guardian

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.io.Serializable
import java.util.*
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.Device
import org.rfcx.audiomoth.entity.SyncState

@RealmClass
open class GuardianDeployment(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var deployedAt: Date = Date(),
    @Expose(serialize = false)
    var state: Int = 0, // 1 = Locate, 2 = Config, 3 = Sync, 4 = Verify, 5 = Deploy, 6 = Ready To Upload
    var device: String? = Device.GUARDIAN.value,
    var wifiName: String? = null,
    var configuration: GuardianConfiguration? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date = Date(),
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel, Serializable {

    fun isSent(): Boolean = (syncState == SyncState.Sent.key)

    companion object {
        const val FIELD_ID = "id"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_STATE = "state"
        const val FIELD_SYNC_STATE = "syncState"
    }
}
