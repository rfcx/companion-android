package org.rfcx.companion.entity

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class TrackingFile(
    @PrimaryKey
    var id: Int = 0,
    var deploymentId: Int = 0,
    var deploymentServerId: String? = null,
    var siteId: Int = 0,
    var siteServerId: String? = null,
    var localPath: String = "",
    var remotePath: String? = null,
    @Expose(serialize = false)
    var syncState: Int = 0,
    var device: String = ""
) : RealmModel {
    companion object {
        const val TABLE_NAME = "TrackingFile"
        const val FIELD_ID = "id"
        const val FIELD_DEPLOYMENT_ID = "deploymentId"
        const val FIELD_DEPLOYMENT_SERVER_ID = "deploymentServerId"
        const val FIELD_SITE_ID = "siteId"
        const val FIELD_SITE_SERVER_ID = "siteServerId"
        const val FIELD_SYNC_STATE = "syncState"
        const val FIELD_LOCAL_PATH = "localPath"
        const val FIELD_REMOTE_PATH = "remotePath"
        const val FIELD_DEVICE = "device"
    }
}
