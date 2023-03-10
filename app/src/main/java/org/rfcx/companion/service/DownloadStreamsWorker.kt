package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rfcx.companion.entity.response.convertToDeploymentResponse
import org.rfcx.companion.localdb.DeploymentDb
import org.rfcx.companion.localdb.StreamDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.getIdToken

class DownloadStreamsWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private var count = 0
    private var currentStreamsLoading = 0
    private var someFailed = false

    override suspend fun doWork(): Result {
        // reset to default
        count = 0
        currentStreamsLoading = 0
        someFailed = false

        if (DeploymentSyncWorker.isRunning() != DeploymentSyncState.RUNNING) {
            Log.d(TAG, "doWork on DownloadStreams")

            val token = "Bearer ${context.getIdToken()}"
            val result = getStreams(token, currentStreamsLoading)
            if (result) {
                Log.d(TAG, "downloaded $count sites")
                isRunning = DownloadStreamState.FINISH
                // Trigger delete streams after download streams successfully
                DeleteStreamsWorker.enqueue(context, PROJECT_ID)
            } else {
                isRunning = DownloadStreamState.NOT_RUNNING
                someFailed = true
            }
            return if (someFailed) Result.retry() else Result.success()
        }
        PROJECT_ID = null
        return Result.retry()
    }

    private suspend fun getStreams(token: String, offset: Int, maxUpdatedAt: String? = null): Boolean = withContext(Dispatchers.IO) {
        isRunning = DownloadStreamState.RUNNING
        val projectId = PROJECT_ID?.let { listOf(it) }
        val result = ApiManager.getInstance().getDeviceApi(context)
            .getStreams(token, SITES_LIMIT_GETTING, offset, maxUpdatedAt, "updated_at,name", projectId).execute()
        if (result.isSuccessful) {
            val resultBody = result.body()
            resultBody?.let {
                val streamDb = StreamDb(Realm.getInstance(RealmHelper.migrationConfig()))
                val deploymentDb = DeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
                count += resultBody.size
                streamDb.insertOrUpdate(it)

                // insert deployments
                val deploymentStreams = resultBody.filter { st -> st.deployment != null }
                deploymentDb.insertOrUpdate(deploymentStreams.map { st -> st.convertToDeploymentResponse() })

                if (it.size == SITES_LIMIT_GETTING) {
                    if (streamDb.getMaxUpdatedAt() == maxUpdatedAt) {
                        currentStreamsLoading += SITES_LIMIT_GETTING
                    } else {
                        currentStreamsLoading = 0
                    }
                    return@withContext getStreams(token, currentStreamsLoading, streamDb.getMaxUpdatedAt())
                } else {
                    return@withContext true
                }
            }
        } else {
            return@withContext false
        }
        return@withContext true
    }

    companion object {
        private const val TAG = "DownloadStreamsWorker"
        private const val UNIQUE_WORK_KEY = "DownloadStreamsWorkerUniqueKey"
        private const val SITES_LIMIT_GETTING = 100
        private var isRunning = DownloadStreamState.NOT_RUNNING
        private var PROJECT_ID: String? = null

        fun enqueue(context: Context, projectId: String) {
            PROJECT_ID = projectId
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<DownloadStreamsWorker>().setConstraints(constraints)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_KEY, ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun isRunning() = isRunning

        fun workInfos(context: Context): LiveData<List<WorkInfo>> {
            return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_KEY)
        }
    }
}

enum class DownloadStreamState { NOT_RUNNING, RUNNING, FINISH }
