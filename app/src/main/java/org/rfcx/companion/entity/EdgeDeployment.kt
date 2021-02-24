package org.rfcx.companion.entity

import com.google.gson.annotations.Expose
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.rfcx.companion.util.randomDeploymentId
import java.util.*

@RealmClass
open class EdgeDeployment(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var deployedAt: Date = Date(),
    var deploymentKey: String? = randomDeploymentId(), // random when edge
    @Expose(serialize = false)
    var state: Int = 0, // 1 = Locate, 2 = Config, 3 = Deploy, 4 = Ready To Upload
    var stream: DeploymentLocation? = null,
    var createdAt: Date = Date(),
    @Expose(serialize = false)
    var syncState: Int = 0,
    var updatedAt: Date? = null,
    var deletedAt: Date? = null,
    var isActive: Boolean = true,
    var passedChecks: RealmList<Int>? = null
) : RealmModel {

    fun isCompleted(): Boolean {
        return deletedAt == null && isActive
    }

    companion object {
        const val TABLE_NAME = "EdgeDeployment"
        const val FIELD_ID = "id"
        const val FIELD_STATE = "state"
        const val FIELD_SYNC_STATE = "syncState"
        const val FIELD_SERVER_ID = "serverId"
        const val FIELD_DEPLOYMENT_KEY = "deploymentKey"
        const val FIELD_STREAM = "stream"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_DELETED_AT = "deletedAt"
        const val FIELD_PASSED_CHECKS = "passedChecks"
    }
}
