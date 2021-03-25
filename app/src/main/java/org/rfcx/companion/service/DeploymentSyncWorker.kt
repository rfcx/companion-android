package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.companion.entity.request.EditDeploymentRequest
import org.rfcx.companion.entity.request.toRequestBody
import org.rfcx.companion.entity.response.toEdgeDeployment
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.service.images.ImageSyncWorker
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.getIdToken

/**
 * For syncing data to server. Ref from Ranger Android App
 */
class DeploymentSyncWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork")

        val db = EdgeDeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val locateDb = LocateDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val deployments = db.lockUnsent()
        val token = "Bearer ${context.getIdToken()}"

        Log.d(TAG, "doWork: found ${deployments.size} unsent")
        var someFailed = false

        deployments.forEach {
            Log.d(TAG, "doWork: sending id ${it.id}")

            if (it.serverId == null) {
                val result = ApiManager.getInstance().getDeviceApi()
                    .createDeployment(token, it.toRequestBody()).execute()

                if (result.isSuccessful) {
                    val fullId = result.headers().get("Location")
                    val id = fullId?.substring(fullId.lastIndexOf("/") + 1, fullId.length) ?: ""
                    db.markSent(id, it.id)

                    //update core siteId when deployment created
                    val updatedDp = ApiManager.getInstance().getDeviceApi()
                        .getDeployment(token, id).execute().body()
                    updatedDp?.let { dp ->
                        db.updateDeploymentByServerId(updatedDp.toEdgeDeployment())
                        locateDb.updateSiteServerId(it.id, dp.stream!!.id!!)
                    }

                    //send tracking if there is
                    TrackingSyncWorker.enqueue(context)
                } else {
                    db.markUnsent(it.id)
                    someFailed = true
                }
            } else {
                val deploymentLocation = it.stream
                val serverId = it.serverId ?: ""
                deploymentLocation?.let { location ->
                    if (it.deletedAt != null) {
                        val result = ApiManager.getInstance().getDeviceApi()
                            .deleteDeployments(token, serverId).execute()
                        if (result.isSuccessful) {
                            db.deleteDeployment(it.id)
                        }
                    } else {
                        val req = EditDeploymentRequest(location.toRequestBody())
                        val result = ApiManager.getInstance().getDeviceApi()
                            .editDeployments(token, serverId, req).execute()
                        if (result.isSuccessful) {
                            db.markSent(it.serverId!!, it.id)
                        }
                    }
                }
            }
        }

        ImageSyncWorker.enqueue(context)

        return if (someFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "DeploymentSyncWorker"
        private const val UNIQUE_WORK_KEY = "DeploymentSyncWorkerUniqueKey"

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<DeploymentSyncWorker>().setConstraints(constraints)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_KEY, ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun workInfos(context: Context): LiveData<List<WorkInfo>> {
            return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_KEY)
        }
    }
}
