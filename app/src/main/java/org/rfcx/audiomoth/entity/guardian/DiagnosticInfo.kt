package org.rfcx.audiomoth.entity.guardian

import android.text.format.DateUtils
import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class DiagnosticInfo(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var deploymentServerId: String? = null,
    var lastConnection: Date = Date(),
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel {

    companion object {
        const val FIELD_ID = "id"
        const val FIELD_SYNC_STATE = "syncState"
        const val FIELD_DEPLOY_ID = "deploymentServerId"
        const val FIELD_SERVER_ID = "serverId"
    }
}

fun DiagnosticInfo.getRelativeTimeSpan(): String {
    val lastConnection = this.lastConnection.time
    val currentTime = System.currentTimeMillis()
    return DateUtils.getRelativeTimeSpanString(lastConnection, currentTime, DateUtils.MINUTE_IN_MILLIS).toString()
}
