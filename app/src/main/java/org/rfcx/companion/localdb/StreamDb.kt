package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.TrackingFile
import org.rfcx.companion.entity.response.StreamResponse
import org.rfcx.companion.entity.response.toStream
import org.rfcx.companion.util.toISO8601Format

class StreamDb(private val realm: Realm) {

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Stream> {
        return realm.where(Stream::class.java)
            .sort(Stream.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getAllResultsAsyncWithinProject(
        sort: Sort = Sort.DESCENDING,
        id: Int
    ): RealmResults<Stream> {
        return realm.where(Stream::class.java)
            .equalTo("project.id", id)
            .sort(Stream.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getStreams(): List<Stream> {
        val streams = realm.where(Stream::class.java).findAll()
        return realm.copyFromRealm(streams) ?: listOf()
    }

    fun getStreamById(id: Int): Stream? {
        return realm.where(Stream::class.java).equalTo(Stream.FIELD_ID, id).findFirst()
    }

    fun deleteStream(id: Int) {
        realm.executeTransaction {
            val locate =
                it.where(Stream::class.java).equalTo(Stream.FIELD_ID, id)
                    .findFirst()
            locate?.deleteFromRealm()
        }
    }

    fun updateDeploymentIdOnStream(deploymentId: Int, streamId: Int) {
        realm.executeTransaction {
            val stream =
                realm.where(Stream::class.java).equalTo(Stream.FIELD_ID, streamId).findFirst()
            if (stream != null) {
                stream.lastDeploymentId = deploymentId
            }
        }
    }

    fun updateSiteServerId(deploymentId: Int, siteServerId: String, deploymentServerId: String) {
        realm.executeTransaction {
            // update server id in track
            it.where(TrackingFile::class.java)
                .equalTo(TrackingFile.FIELD_DEPLOYMENT_ID, deploymentId)
                .findFirst()?.apply {
                    this.siteServerId = siteServerId
                    this.deploymentServerId = deploymentServerId
                }

            // update server id in site
            it.where(Stream::class.java)
                .equalTo(Stream.FIELD_LAST_DEPLOYMENT_ID, deploymentId)
                .findFirst()?.apply {
                    this.serverId = siteServerId
                    this.syncState = SyncState.Sent.key
                }
        }
    }

    fun insertOrUpdate(stream: Stream): Int {
        var id = stream.id
        realm.executeTransaction {
            if (stream.id == 0) {
                id = (realm.where(Stream::class.java).max(Stream.FIELD_ID)?.toInt() ?: 0) + 1
                stream.id = id
            }
            it.insertOrUpdate(stream)
        }
        return id
    }

    fun updateValues(
        id: Int,
        name: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        altitude: Double? = null,
        projectId: Int? = null
    ) {
        realm.executeTransaction {
            val stream = it.where(Stream::class.java).equalTo(Stream.FIELD_ID, id).findFirst()
            if (name != null) stream?.name = name
            if (latitude != null) stream?.latitude = latitude
            if (longitude != null) stream?.longitude = longitude
            if (altitude != null) stream?.altitude = altitude
            if (projectId != null) {
                val project =
                    it.where(Project::class.java).equalTo(Project.PROJECT_ID, projectId).findFirst()
                stream?.project = project
            }
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
                    val project = it.where(Project::class.java)
                        .equalTo(Project.PROJECT_SERVER_ID, streamResponse.project?.id).findFirst()
                    locate.project = project

                    val id = (it.where(Stream::class.java).max(Stream.FIELD_ID)?.toInt() ?: 0) + 1
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

                    val project = it.where(Project::class.java)
                        .equalTo(Project.PROJECT_SERVER_ID, streamResponse.project?.id).findFirst()
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
