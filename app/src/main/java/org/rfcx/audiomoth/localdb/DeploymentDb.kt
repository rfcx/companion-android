package org.rfcx.audiomoth.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.Locate

class DeploymentDb(private val realm: Realm) {

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Deployment> {
        return realm.where(Deployment::class.java)
            .sort(Deployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun insertOrUpdateDeployment(deployment: Deployment, locate: Locate): Int {
        var id = deployment.id
        realm.executeTransaction {
            if (deployment.id == 0) {
                id = (realm.where(Deployment::class.java).max(Deployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deployment.id = id
            }
            deployment.location = locate.asDeploymentLocation() // add deploy location
            it.insertOrUpdate(deployment)
        }
        return id
    }

    fun getDeploymentById(id: Int): Deployment? {
        return realm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id).findFirst()
    }
}