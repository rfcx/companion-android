package org.rfcx.audiomoth.localdb

import io.realm.Realm
import org.rfcx.audiomoth.entity.Deployment

class DeploymentDb(private val realm: Realm) {

    fun saveDeployment(deployment: Deployment) {
        realm.use { it ->
            it.executeTransaction {
                it.insertOrUpdate(deployment)
            }
        }
    }

    fun getDeploymentById(id: Int): Deployment? {
        return realm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id).findFirst()
    }
}