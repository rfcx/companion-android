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
}