package org.rfcx.companion.entity.guardian

import android.content.Context
import com.google.gson.annotations.Expose
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.rfcx.companion.R
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.DeploymentState
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.util.Pin
import org.rfcx.companion.util.randomDeploymentId
import org.rfcx.companion.view.map.MapMarker
import java.io.Serializable
import java.util.*

@RealmClass
open class Deployment(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var deployedAt: Date = Date(),
    var deploymentKey: String = randomDeploymentId(),
    @Expose(serialize = false)
    var state: Int = 0, // 1 = Locate, 2 = Config, 3 = Sync, 4 = Verify, 5 = Deploy, 6 = Ready To Upload
    var device: String? = Device.GUARDIAN.value,
    var stream: DeploymentLocation? = null,
    var createdAt: Date = Date(),
    var updatedAt: Date? = null,
    var isActive: Boolean = false,
    @Expose(serialize = false)
    var syncState: Int = 0,
    var deletedAt: Date? = null,
    var passedChecks: RealmList<Int>? = null,
    var deviceParameters: String? = null
) : RealmModel, Serializable {

    fun isCompleted(): Boolean {
        return isActive && (state == DeploymentState.AudioMoth.ReadyToUpload.key)
    }

    fun isUnsynced(): Boolean {
        return isActive && syncState != SyncState.Sent.key
    }

    companion object {
        const val TABLE_NAME = "Deployment"
        const val FIELD_ID = "id"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_STATE = "state"
        const val FIELD_SYNC_STATE = "syncState"
        const val FIELD_STREAM = "stream"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_DELETED_AT = "deletedAt"
        const val FIELD_DEVICE = "device"
        const val FIELD_PASSED_CHECKS = "passedChecks"
        const val FIELD_DEVICE_PARAMETERS = "deviceParameters"
    }
}

fun Deployment.toMark(context: Context): MapMarker.DeploymentMarker {
    val pinImage = when (state) {
        DeploymentState.AudioMoth.ReadyToUpload.key -> {
            Pin.PIN_GREEN
        }
        DeploymentState.Guardian.ReadyToUpload.key -> {
            Pin.PIN_GREEN
        }
        else -> {
            Pin.PIN_GREY
        }
    }

    val description = if (state >= DeploymentState.AudioMoth.ReadyToUpload.key)
        context.getString(R.string.format_deployed)
    else
        context.getString(R.string.format_in_progress_step)

    return MapMarker.DeploymentMarker(
        id,
        stream?.name ?: "",
        stream?.longitude ?: 0.0,
        stream?.latitude ?: 0.0,
        pinImage,
        description,
        device ?: "",
        stream?.project?.name ?: "",
        deploymentKey,
        createdAt,
        deployedAt,
        updatedAt
    )
}
