package org.rfcx.companion.entity

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class DeploymentImage(
    @PrimaryKey
    var id: Int = 0,
    @Expose(serialize = false)
    var deploymentId: Int = 0,
    var deploymentServerId: String? = null,
    @Expose(serialize = false)
    var localPath: String = "",
    var remotePath: String? = null,
    var createdAt: Date = Date(),
    var device: String = "",
    @Expose(serialize = false)
    var syncState: Int = 0,
    @Expose(serialize = false)
    var syncToFireStoreState: Int = 0 // TODO: no need anymore
) : RealmModel {
    companion object {
        const val TABLE_NAME = "DeploymentImage"
        const val FIELD_ID = "id"
        const val FIELD_DEPLOYMENT_ID = "deploymentId"
        const val FIELD_SYNC_STATE = "syncState"
        const val FIELD_REMOTE_PATH = "remotePath"
        const val FIELD_LOCAL_PATH = "localPath"
        const val FIELD_DEVICE = "device"
        const val FIELD_DEPLOYMENT_SERVER_ID = "deploymentServerId"
    }
}
