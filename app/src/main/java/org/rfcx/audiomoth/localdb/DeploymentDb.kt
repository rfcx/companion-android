package org.rfcx.audiomoth.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.entity.SyncState

/**
 * For Manage the saving and sending of deployment from the local database
 */
class DeploymentDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(Deployment::class.java)
            .equalTo(Deployment.FIELD_STATE, DeploymentState.ReadyToUpload.key)
            .and()
            .notEqualTo(Deployment.FIELD_SYNC_STATE, SyncState.Uploaded.key)
            .count()
    }

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Deployment> {
        return realm.where(Deployment::class.java)
            .sort(Deployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun insertOrUpdateDeployment(deployment: Deployment, location: DeploymentLocation): Int {
        var id = deployment.id
        realm.executeTransaction {
            if (deployment.id == 0) {
                id = (realm.where(Deployment::class.java).max(Deployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deployment.id = id
            }
            deployment.location = location // add deploy location
            it.insertOrUpdate(deployment)
        }
        return id
    }

    fun updateDeployment(deployment: Deployment) {
        realm.executeTransaction {
            it.insertOrUpdate(deployment)
        }

    }

    fun getDeploymentById(id: Int): Deployment? {
        return realm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id).findFirst()
    }
}