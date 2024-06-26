package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.companion.entity.UnsyncedDeployment
import org.rfcx.companion.entity.request.EditDeploymentRequest
import org.rfcx.companion.entity.request.toRequestBody
import org.rfcx.companion.localdb.DeploymentDb
import org.rfcx.companion.localdb.StreamDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.service.images.ImageSyncWorker
import org.rfcx.companion.util.RealmHelper
import java.util.*
import java.util.concurrent.ExecutionException

/**
 * For syncing data to server. Ref from Ranger Android App
 */
class DeploymentSyncWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        errors.clear()
        Log.d(TAG, "doWork")
        isRunning = DeploymentSyncState.RUNNING

        val db = DeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val locateDb = StreamDb(Realm.getInstance(RealmHelper.migrationConfig()))
        db.unlockSending()
        val deployments = db.lockUnsent()

        Log.d(TAG, "doWork: found ${deployments.size} unsent")
        var someFailed = false

        deployments.forEach {
            Log.d(TAG, "doWork: sending id ${it.id}")

            try {
                if (it.serverId == null) {
                    val result = ApiManager.getInstance().getDeviceApi(context)
                        .createDeployment(it.toRequestBody()).execute()

                    val error = result.errorBody()?.string()
                    when {
                        result.isSuccessful -> {
                            val fullId = result.headers().get("Location")
                            val id = fullId?.substring(fullId.lastIndexOf("/") + 1, fullId.length) ?: ""
                            markSentDeployment(id, db, locateDb, it.id)
                        }
                        error?.contains("this deploymentKey is already existed") ?: false -> {
                            markSentDeployment(it.deploymentKey, db, locateDb, it.id)
                        }
                        else -> {
                            errors.add(
                                UnsyncedDeployment(
                                    it.id,
                                    it.stream!!.name,
                                    it.deployedAt,
                                    (error ?: "Unexpected error")
                                )
                            )
                            db.markUnsent(it.id)
                            someFailed = true
                        }
                    }
                } else {
                    val deploymentLocation = it.stream
                    val serverId = it.serverId ?: ""
                    deploymentLocation?.let { location ->
                        if (it.deletedAt != null) {
                            val result = ApiManager.getInstance().getDeviceApi(context)
                                .deleteDeployments(serverId).execute()
                            if (result.isSuccessful) {
                                db.deleteDeployment(it.id)
                            }
                        } else {
                            val req = EditDeploymentRequest(location.toRequestBody())
                            val result = ApiManager.getInstance().getDeviceApi(context)
                                .editDeployments(serverId, req).execute()
                            if (result.isSuccessful) {
                                db.markSent(it.serverId!!, it.id)
                            } else {
                                errors.add(
                                    UnsyncedDeployment(
                                        it.id,
                                        it.stream!!.name,
                                        it.deployedAt,
                                        (result.errorBody()?.string() ?: "Unexpected error")
                                    )
                                )
                                db.markUnsent(it.id)
                                someFailed = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                someFailed = true
            }
        }
        isRunning = DeploymentSyncState.FINISH
        ImageSyncWorker.enqueue(context)

        return if (someFailed) Result.failure() else Result.success()
    }

    private fun markSentDeployment(
        id: String,
        db: DeploymentDb,
        streamDb: StreamDb,
        deploymentId: Int
    ) {
        db.markSent(id, deploymentId)

        // update core siteId when deployment created
        val updatedDp = ApiManager.getInstance().getDeviceApi(context)
            .getDeployment(id).execute().body()
        updatedDp?.let { dp ->
            streamDb.updateSiteServerId(deploymentId, dp.stream!!.id!!, id)
        }

        // send tracking if there is
        TrackingSyncWorker.enqueue(context)
    }

    companion object {
        private const val TAG = "DeploymentSyncWorker"
        private const val UNIQUE_WORK_KEY = "DeploymentSyncWorkerUniqueKey"
        private var isRunning = DeploymentSyncState.NOT_RUNNING

        private var errors = mutableListOf<UnsyncedDeployment>()

        fun enqueue(context: Context) {
            // won't enqueue if worker is running
            if (!isWorkRunning(context)) {
                val constraints =
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                val workRequest =
                    OneTimeWorkRequestBuilder<DeploymentSyncWorker>().setConstraints(constraints)
                        .build()
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(UNIQUE_WORK_KEY, ExistingWorkPolicy.KEEP, workRequest)
            }
        }

        fun isRunning() = isRunning

        fun getErrors() = errors

        private fun isWorkRunning(context: Context): Boolean {

            val instance = WorkManager.getInstance(context)
            val statuses = instance.getWorkInfosForUniqueWork(UNIQUE_WORK_KEY)

            var running = false
            var workInfoList = Collections.emptyList<WorkInfo>()

            try {
                workInfoList = statuses.get()
            } catch (e: ExecutionException) {
                Log.d(TAG, "ExecutionException in isWorkScheduled: $e")
            } catch (e: InterruptedException) {
                Log.d(TAG, "InterruptedException in isWorkScheduled: $e")
            }

            workInfoList.forEach {
                val state = it.state
                running = state == WorkInfo.State.RUNNING
                Log.d(TAG, "now worker running state is $running")
            }
            return running
        }

        fun workInfos(context: Context): LiveData<List<WorkInfo>> {
            return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_KEY)
        }
    }
}

enum class DeploymentSyncState { NOT_RUNNING, RUNNING, FINISH }
