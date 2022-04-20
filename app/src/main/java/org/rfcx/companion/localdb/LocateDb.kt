package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.response.StreamResponse
import org.rfcx.companion.entity.response.toProject
import org.rfcx.companion.entity.response.toStream
import org.rfcx.companion.util.toISO8601Format

class LocateDb(private val realm: Realm) {

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Stream> {
        return realm.where(Stream::class.java)
            .sort(Stream.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getAllResultsAsyncWithinProject(
        sort: Sort = Sort.DESCENDING,
        project: String
    ): RealmResults<Stream> {
        return realm.where(Stream::class.java)
            .equalTo("locationGroup.name", project)
            .sort(Stream.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getLocations(): List<Stream> {
        return realm.where(Stream::class.java).findAll() ?: arrayListOf()
    }

    fun getLocateByName(name: String): Stream? {
        return realm.where(Stream::class.java).equalTo(Stream.FIELD_NAME, name).findFirst()
    }

    fun getLocateById(id: Int): Stream? {
        return realm.where(Stream::class.java).equalTo(Stream.FIELD_ID, id).findFirst()
    }

    fun deleteLocate(id: Int) {
        realm.executeTransaction {
            val locate =
                it.where(Stream::class.java).equalTo(Stream.FIELD_ID, id)
                    .findFirst()
            locate?.deleteFromRealm()
        }
    }

    fun insertOrUpdateLocate(deploymentId: Int, stream: Stream) {
        realm.executeTransaction {
            if (stream.id == 0) {
                val id = (
                    realm.where(Stream::class.java).max(Stream.FIELD_ID)
                        ?.toInt() ?: 0
                    ) + 1
                stream.id = id
            }
            stream.lastDeploymentId = deploymentId
            it.insertOrUpdate(stream)
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
            it.where(Stream::class.java)
                .equalTo(Stream.FIELD_LAST_DEPLOYMENT_ID, deploymentId)
                .findFirst()?.apply {
                    this.serverId = serverId
                }
        }
    }

    fun insertOrUpdate(stream: Stream) {
        realm.executeTransaction {
            if (stream.id == 0) {
                val id = (
                    realm.where(Stream::class.java).max(Stream.FIELD_ID)
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
                    it.where(Stream::class.java)
                        .equalTo(Stream.FIELD_SERVER_ID, streamResponse.id)
                        .findFirst()

                if (location == null) {
                    val locate = streamResponse.toStream()
                    val project = it.where(Project::class.java).equalTo(Project.PROJECT_SERVER_ID, streamResponse.project?.id).findFirst()
                    locate.project = project

                    val id = (
                        it.where(Stream::class.java).max(Stream.FIELD_ID)
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

                    val project = it.where(Project::class.java).equalTo(Project.PROJECT_SERVER_ID, streamResponse.project?.id).findFirst()
                    location.project = project
                }
            }
        }
    }

    fun getMaxUpdatedAt(): String? {
        return realm.where(Stream::class.java).isNotNull(Stream.FIELD_SERVER_ID)
            .isNotNull(Stream.FIELD_UPDATED_AT).findAll()
            .maxByOrNull { it.updatedAt!! }?.updatedAt?.toISO8601Format()
    }
}
