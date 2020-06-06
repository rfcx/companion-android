package org.rfcx.audiomoth.localdb

import io.realm.Realm
import org.rfcx.audiomoth.entity.DeploymentLocation

class DeploymentLocationDb(private val realm: Realm) {

    fun getLocations(): List<DeploymentLocation> {
        return realm.where(DeploymentLocation::class.java).findAll() ?: arrayListOf()
    }

    fun getDeploymentLocationById(id: Int): DeploymentLocation? {
        return realm.where(DeploymentLocation::class.java).equalTo(DeploymentLocation.FIELD_ID, id)
            .findFirst()
    }

    fun saveDeploymentLocation(deploymentLocation: DeploymentLocation): Int {
        var id = deploymentLocation.id
        realm.executeTransaction {
            if (deploymentLocation.id == 0) {
                id = (realm.where(DeploymentLocation::class.java).max(DeploymentLocation.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deploymentLocation.id = id
            }
            it.insertOrUpdate(deploymentLocation)
        }
        return id
    }
}