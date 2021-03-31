package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import java.io.File

class TrackingFileDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(TrackingFile::class.java)
            .notEqualTo(TrackingFile.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun unlockSending() {
        realm.executeTransaction {
            val snapshot = it.where(TrackingFile::class.java)
                .equalTo(TrackingFile.FIELD_SYNC_STATE, SyncState.Sending.key).findAll()
                .createSnapshot()
            snapshot.forEach { file ->
                file.syncState = SyncState.Unsent.key
            }
        }
    }

    fun lockUnsent(): List<TrackingFile> {
        var unsentCopied: List<TrackingFile> = listOf()
        realm.executeTransaction {
            val unsent = it.where(TrackingFile::class.java)
                .equalTo(TrackingFile.FIELD_SYNC_STATE, SyncState.Unsent.key)
                .isNotNull(TrackingFile.FIELD_DEPLOYMENT_SERVER_ID)
                .findAll()
                .createSnapshot()
            unsentCopied = unsent.toList()
            unsent.forEach { d ->
                d.syncState = SyncState.Sending.key
            }
        }
        return unsentCopied
    }

    fun markUnsent(id: Int) {
        realm.executeTransaction {
            val file =
                it.where(TrackingFile::class.java).equalTo(TrackingFile.FIELD_ID, id).findFirst()
            if (file != null) {
                file.syncState = SyncState.Unsent.key
            }
        }
    }

    fun markSent(id: Int, remotePath: String?) {
        realm.executeTransaction {
            val file =
                it.where(TrackingFile::class.java).equalTo(TrackingFile.FIELD_ID, id).findFirst()
            if (file != null) {
                file.syncState = SyncState.Sent.key
                file.remotePath = remotePath
            }
        }
    }

    fun getTrackingFileBySiteId(id: Int): RealmResults<TrackingFile> {
        return realm.where(TrackingFile::class.java)
            .equalTo(TrackingFile.FIELD_SITE_ID, id)
            .findAll()
    }

    fun insertOrUpdate(file: TrackingFile) {
        realm.executeTransaction {
            if (file.id == 0) {
                val id =
                    (realm.where(Tracking::class.java).max(Tracking.TRACKING_ID)
                        ?.toInt() ?: 0) + 1
                file.id = id
            }
            it.insertOrUpdate(file)
        }
    }

    fun insertOrUpdate(
        deploymentAssetResponse: DeploymentAssetResponse,
        filePath: String,
        deploymentId: Int?,
        device: String
    ) {
        realm.executeTransaction {
            val file =
                it.where(TrackingFile::class.java)
                    .equalTo(TrackingFile.FIELD_REMOTE_PATH, "assets/${deploymentAssetResponse.id}")
                    .findFirst()

            if (file == null && deploymentId != null) {
                val deploymentTracking = deploymentAssetResponse.toDeploymentTrack()
                val id = (it.where(TrackingFile::class.java).max(TrackingFile.FIELD_ID)?.toInt()
                    ?: 0) + 1
                deploymentTracking.id = id
                deploymentTracking.siteId = deploymentId
                deploymentTracking.syncState = SyncState.Sent.key
                deploymentTracking.localPath = filePath
                deploymentTracking.device = device
                it.insert(deploymentTracking)
            } else {
                File(filePath).delete()
            }
        }
    }
}
