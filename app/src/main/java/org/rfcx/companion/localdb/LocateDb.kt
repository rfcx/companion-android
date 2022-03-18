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

    fun getAllResultsAsyncWithinProject(
        sort: Sort = Sort.DESCENDING,
        project: String
    ): RealmResults<Locate> {
        return realm.where(Locate::class.java)
            .equalTo("locationGroup.name", project)
            .sort(Locate.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getLocations(): List<Locate> {
        return realm.where(Locate::class.java).findAll() ?: arrayListOf()
    }

    fun getLocateByName(name: String): Locate? {
        return realm.where(Locate::class.java).equalTo(Locate.FIELD_NAME, name).findFirst()
    }

    fun getLocateById(id: Int): Locate? {
        return realm.where(Locate::class.java).equalTo(Locate.FIELD_ID, id).findFirst()
    }

    fun deleteLocate(id: Int) {
        realm.executeTransaction {
            val locate =
                it.where(Locate::class.java).equalTo(Locate.FIELD_ID, id)
                    .findFirst()
            locate?.deleteFromRealm()
        }
    }

    fun insertOrUpdateLocate(deploymentId: Int, locate: Locate) {
        realm.executeTransaction {
            if (locate.id == 0) {
                val id = (
                    realm.where(Locate::class.java).max(Locate.FIELD_ID)
                        ?.toInt() ?: 0
                    ) + 1
                locate.id = id
            }
            locate.lastDeploymentId = deploymentId
            it.insertOrUpdate(locate)
        }
    }

    fun updateSiteServerId(deploymentId: Int, serverId: String) {
        realm.executeTransaction {
            // update server id in track
            it.where(TrackingFile::class.java)
                .equalTo(TrackingFile.FIELD_DEPLOYMENT_ID, deploymentId)
                .findFirst()?.apply {
                    this.siteServerId = serverId
                }

            // update server id in site
            it.where(Locate::class.java)
                .equalTo(Locate.FIELD_LAST_DEPLOYMENT_ID, deploymentId)
                .findFirst()?.apply {
                    this.serverId = serverId
                }
        }
    }

    fun insertOrUpdate(stream: Locate) {
        realm.executeTransaction {
            if (stream.id == 0) {
                val id = (
                    realm.where(Locate::class.java).max(Locate.FIELD_ID)
                        ?.toInt() ?: 0
                    ) + 1
                stream.id = id
            }
            it.insertOrUpdate(stream)
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
                    val id = (
                        it.where(Locate::class.java).max(Locate.FIELD_ID)
                            ?.toInt() ?: 0
                        ) + 1
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
        return realm.where(Locate::class.java).isNotNull(Locate.FIELD_SERVER_ID)
            .isNotNull(Locate.FIELD_UPDATED_AT).findAll()
            .maxByOrNull { it.updatedAt!! }?.updatedAt?.toISO8601Format()
    }
}
