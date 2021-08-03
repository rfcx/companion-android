package org.rfcx.companion.entity.guardian

import android.content.Context
import com.google.gson.annotations.Expose
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.DeploymentState
import org.rfcx.companion.entity.Device
import org.rfcx.companion.util.GuardianPin
import org.rfcx.companion.util.WifiHotspotUtils
import org.rfcx.companion.util.randomDeploymentId
import org.rfcx.companion.view.map.MapMarker
import java.io.Serializable
import java.util.*

@RealmClass
open class GuardianDeployment(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var deployedAt: Date = Date(),
    var deploymentKey: String = randomDeploymentId(),
    @Expose(serialize = false)
    var state: Int = 0, // 1 = Locate, 2 = Config, 3 = Sync, 4 = Verify, 5 = Deploy, 6 = Ready To Upload
    var device: String? = Device.GUARDIAN.value,
    var wifiName: String? = "",
    var configuration: GuardianConfiguration? = null,
    var stream: DeploymentLocation? = null,
    var createdAt: Date = Date(),
    var updatedAt: Date? = null,
    var isActive: Boolean = false,
    @Expose(serialize = false)
    var syncState: Int = 0,
    var deletedAt: Date? = null,
    var passedChecks: RealmList<Int>? = null
) : RealmModel, Serializable {

    fun isCompleted(): Boolean {
        return isActive && state == DeploymentState.Guardian.ReadyToUpload.key
    }

    companion object {
        const val TABLE_NAME = "GuardianDeployment"
        const val FIELD_ID = "id"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_STATE = "state"
        const val FIELD_SYNC_STATE = "syncState"
        const val FIELD_STREAM = "stream"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_DELETED_AT = "deletedAt"
        const val FIELD_PASSED_CHECKS = "passedChecks"
    }
}

fun GuardianDeployment.toMark(context: Context): MapMarker.DeploymentMarker {
    val color = stream?.project?.color
    val pinImage =
        if (state == DeploymentState.Guardian.ReadyToUpload.key) {
            if (WifiHotspotUtils.isConnectedWithGuardian(context, this.wifiName!!)) {
                if (color != null && color.isNotEmpty()) {
                    stream?.project?.color
                } else {
                    GuardianPin.CONNECTED_GUARDIAN
                }
            } else {
                GuardianPin.CONNECTED_GUARDIAN
            }
        } else {
            GuardianPin.CONNECTED_GUARDIAN
        } ?: GuardianPin.CONNECTED_GUARDIAN
    return MapMarker.DeploymentMarker(
        id,
        stream?.name ?: "",
        stream?.longitude ?: 0.0,
        stream?.latitude ?: 0.0,
        pinImage,
        "-",
        Device.GUARDIAN.value,
        stream?.project?.name ?: "",
        deploymentKey,
        createdAt,
        deployedAt,
        updatedAt
    )
}
