package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.LocationGroup
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.TrackingFile
import org.rfcx.companion.entity.response.StreamResponse
import org.rfcx.companion.entity.response.toLocate
import org.rfcx.companion.util.toISO8601Format

class LocateDb(private val realm: Realm) {

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Locate> {
        return realm.where(Locate::class.java)
            .sort(Locate.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getAll(sort: Sort = Sort.DESCENDING): RealmResults<Locate> {
        return realm.where(Locate::class.java)
            .sort(Locate.FIELD_ID, sort)
            .findAll()
    }

    fun getLocations(): List<Locate> {
        return realm.where(Locate::class.java).findAll() ?: arrayListOf()
    }

    fun getLocateByName(name: String): Locate? {
        return realm.where(Locate::class.java).equalTo(Locate.FIELD_NAME, name).findFirst()
    }

    fun getLocateByEdgeDeploymentId(deploymentId: Int): Locate? {
        return realm.where(Locate::class.java)
            .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_ID, deploymentId).findFirst()
    }

    fun getLocateByServerId(serverId: String): Locate? {
        val locate =
            realm.where(Locate::class.java)
                .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_SERVER_ID, serverId).findFirst()
        if (locate != null) {
            return realm.copyFromRealm(locate)
        }
        return null
    }

    fun getLocateById(id: Int): Locate? {
        return realm.where(Locate::class.java).equalTo(Locate.FIELD_ID, id).findFirst()
    }

    fun getDeleteLocateId(name: String, latitude: Double, longitude: Double): Int? {
        var locateId: Int? = null
        realm.executeTransaction {
            val locate = it.where(Locate::class.java)
                .equalTo("name", name)
                .and()
                .equalTo("latitude", latitude)
                .and()
                .equalTo("longitude", longitude)
                .findFirst()
            locateId = locate?.id
        }
        return locateId
    }

    fun deleteLocate(id: Int) {
        realm.executeTransaction {
            val locate =
                it.where(Locate::class.java).equalTo(Locate.FIELD_ID, id)
                    .findFirst()
            locate?.deleteFromRealm()
        }
    }

    fun insertOrUpdateLocate(deploymentId: Int, locate: Locate, isGuardian: Boolean = false) {
        realm.executeTransaction {
            if (locate.id == 0) {
                val id = (realm.where(Locate::class.java).max(Locate.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                locate.id = id
            }
            // new last deployment?
            if (!isGuardian) {
                locate.lastDeploymentId = deploymentId
                locate.lastGuardianDeploymentId = 0
                locate.lastGuardianDeploymentServerId = null
            } else {
                locate.lastGuardianDeploymentId = deploymentId
                locate.lastDeploymentId = 0
                locate.lastDeploymentServerId = null
            }
            it.insertOrUpdate(locate)
        }
    }

    fun updateDeploymentServerId(
        deploymentId: Int,
        deploymentServerId: String,
        isGuardian: Boolean = false
    ) {
        realm.executeTransaction {
            it.where(Locate::class.java).equalTo(
                if (!isGuardian) "lastDeploymentId" else "lastGuardianDeploymentId",
                deploymentId
            )
                .findFirst()?.apply {
                    if (!isGuardian) {
                        lastDeploymentServerId = deploymentServerId
                    } else {
                        lastGuardianDeploymentServerId = deploymentServerId
                    }
                }
        }
    }

    fun updateSiteServerId(deploymentId: Int, serverId: String) {
        realm.executeTransaction {
            //update server id in site
            it.where(Locate::class.java).equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_ID, deploymentId)
                .findFirst()?.apply {
                    this.serverId = serverId
                }

            //update server id in track
            it.where(TrackingFile::class.java).equalTo(TrackingFile.FIELD_DEPLOYMENT_ID, deploymentId)
                .findFirst()?.apply {
                    this.siteServerId = serverId
                }
        }
    }


    fun insertOrUpdate(streamResponse: StreamResponse) {
        realm.executeTransaction {
            val location =
                it.where(Locate::class.java)
                    .equalTo(Locate.FIELD_SERVER_ID, streamResponse.id)
                    .findFirst()

            if (location == null) {
                val locate = streamResponse.toLocate()
                val id = (it.where(Locate::class.java).max(Locate.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                locate.id = id
                it.insert(locate)
            } else if (location.syncState == SyncState.Sent.key) {
                if (location.serverId == streamResponse.id && location.name == streamResponse.name && location.latitude == streamResponse.latitude && location.longitude == streamResponse.longitude && location.altitude == streamResponse.altitude) {
                    return@executeTransaction
                }

                location.serverId = streamResponse.id
                location.name = streamResponse.name ?: location.name
                location.latitude = streamResponse.latitude ?: location.latitude
                location.longitude = streamResponse.longitude ?: location.longitude
                location.altitude = streamResponse.altitude ?: location.altitude
                location.createdAt = streamResponse.createdAt ?: location.createdAt

                val locationGroupObj = it.createObject(LocationGroup::class.java)
                locationGroupObj?.let { obj ->
                    val locationGroup = streamResponse.project
                    if (locationGroup != null) {
                        obj.name = locationGroup.name
                        obj.color = locationGroup.color
                        obj.coreId = locationGroup.id
                    }
                }
                location.locationGroup = locationGroupObj
            }
        }
    }

    fun insertOrUpdate(streamResponses: List<StreamResponse>) {
        realm.executeTransaction {
            streamResponses.forEach { streamResponse ->
                val location =
                    it.where(Locate::class.java)
                        .equalTo(Locate.FIELD_SERVER_ID, streamResponse.id)
                        .findFirst()

                if (location == null) {
                    val locate = streamResponse.toLocate()
                    val id = (it.where(Locate::class.java).max(Locate.FIELD_ID)
                        ?.toInt() ?: 0) + 1
                    locate.id = id
                    it.insert(locate)
                } else if (location.syncState == SyncState.Sent.key) {

                    location.serverId = streamResponse.id
                    location.name = streamResponse.name ?: location.name
                    location.latitude = streamResponse.latitude ?: location.latitude
                    location.longitude = streamResponse.longitude ?: location.longitude
                    location.altitude = streamResponse.altitude ?: location.altitude
                    location.createdAt = streamResponse.createdAt ?: location.createdAt
                    location.updatedAt = streamResponse.updatedAt ?: location.updatedAt

                    val locationGroupObj = it.createObject(LocationGroup::class.java)
                    locationGroupObj?.let { obj ->
                        val locationGroup = streamResponse.project
                        if (locationGroup != null) {
                            obj.name = locationGroup.name
                            obj.color = locationGroup.color
                            obj.coreId = locationGroup.id
                        }
                    }
                    location.locationGroup = locationGroupObj
                }
            }
        }
    }

    fun getMaxUpdatedAt(): String? {
        return realm.where(Locate::class.java).isNotNull(Locate.FIELD_SERVER_ID).isNotNull(Locate.FIELD_UPDATED_AT).findAll().maxBy { it.updatedAt!! }?.updatedAt?.toISO8601Format()
    }
}
