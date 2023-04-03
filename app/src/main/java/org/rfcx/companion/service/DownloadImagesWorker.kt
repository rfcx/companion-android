package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.companion.localdb.DeploymentDb
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.RealmHelper

class DownloadImagesWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private var someFailed = false

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork on DownloadAssets")
        val deploymentDb = DeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val deployments = deploymentDb.getDeploymentByServerId(deploymentServerId)

        val deployment = when {
            deployments != null -> Triple(deployments.id, deployments.serverId, deployments.device ?: "") // TODO: change to not can be null
            else -> null
        }

        deployment?.let { dp ->
            val result = ApiManager.getInstance().getDeviceApi(context).getDeploymentAssets(dp.second!!).execute()
            if (result.isSuccessful) {
                val dpAssets = result.body()
                dpAssets?.forEach { item ->
                    if (item.mimeType.startsWith("image")) {
                        val deploymentImageDb = DeploymentImageDb(Realm.getInstance(RealmHelper.migrationConfig()))
                        deploymentImageDb.insertOrUpdate(
                            item,
                            dp.first,
                            dp.third,
                            item.meta?.label
                        )
                    }
                }
            } else {
                someFailed = true
            }
        }
        return if (someFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "DownloadAssetsWorker"
        private const val UNIQUE_WORK_KEY = "DownloadAssetsWorkerUniqueKey"
        private var deploymentServerId = ""

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<DownloadImagesWorker>().setConstraints(constraints)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_KEY, ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun enqueue(context: Context, deploymentServerId: String) {
            this.deploymentServerId = deploymentServerId
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<DownloadImagesWorker>().setConstraints(constraints)
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
