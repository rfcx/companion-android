package org.rfcx.audiomoth.entity.guardian

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
    var diagnostic: Diagnostic? = null,
    var createdAt: Date = Date(),
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel {

    companion object {
        const val FIELD_ID = "id"
        const val FIELD_SYNC_STATE = "syncState"
        const val FIELD_SERVER_ID = "serverId"
    }
}
