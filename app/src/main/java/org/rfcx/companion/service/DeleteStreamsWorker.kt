package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rfcx.companion.entity.response.StreamResponse
import org.rfcx.companion.entity.response.toStream
import org.rfcx.companion.localdb.DeploymentDb
import org.rfcx.companion.localdb.StreamDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.getIdToken

class DeleteStreamsWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private var streams = listOf<StreamResponse>()
    private var currentStreamsLoading = 0
    private var someFailed = false

    override suspend fun doWork(): Result {
        // reset to default
        streams = listOf()
        currentStreamsLoading = 0
        someFailed = false

        Log.d(TAG, "doWork on DeleteStreams")

        val result = getStreams(currentStreamsLoading)
        if (result) {
            val streamDb = StreamDb(Realm.getInstance(RealmHelper.migrationConfig()))
            val deploymentDb = DeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
            val savedStreams = streamDb.getStreams().filter { it.serverId != null && it.project?.serverId == PROJECT_ID }
            val downloadedStreams = streams.map { it.toStream().serverId }
            val filteredStreams =
                savedStreams.filter { stream -> !downloadedStreams.contains(stream.serverId) }
            if (filteredStreams.isNotEmpty()) {
                filteredStreams.forEach {
                    Log.d(TAG, "remove stream: ${it.id}")
                    deploymentDb.deleteDeploymentByStreamId(it.serverId!!)
                    streamDb.deleteStream(it.id)
                }
                // force delete deployment on device-api
                DeploymentSyncWorker.enqueue(context)
            }

            val savedUnSyncedStreams = streamDb.getStreams().filter { it.serverId == null && it.project?.serverId == PROJECT_ID }
            // unsynced site which same name (should not have this case in real life but just in case)
            val filteredUnSyncedStreams = savedUnSyncedStreams.filter { stream -> filteredStreams.map { it.name }.contains(stream.name) }
            if (filteredUnSyncedStreams.isNotEmpty()) {
                filteredUnSyncedStreams.forEach {
                    Log.d(TAG, "remove stream: ${it.id}")
                    streamDb.deleteStream(it.id)
                }
            }
        } else {
            someFailed = true
        }
        PROJECT_ID = null
        return if (someFailed) Result.retry() else Result.success()
    }

    private suspend fun getStreams(offset: Int): Boolean =
        withContext(Dispatchers.IO) {
            val projectId = PROJECT_ID?.let { listOf(it) }
            val result = ApiManager.getInstance().getDeviceApi(context)
                .getStreams(SITES_LIMIT_GETTING, offset, null, null, projectId)
                .execute()
            if (result.isSuccessful) {
                val resultBody = result.body()
                resultBody?.let {
                    streams = streams + it
                    if (it.size == SITES_LIMIT_GETTING) {
                        currentStreamsLoading += SITES_LIMIT_GETTING
                        return@withContext getStreams(currentStreamsLoading)
                    }
                }
            } else {
                return@withContext false
            }
            return@withContext true
        }

    companion object {
        private const val TAG = "DeleteStreamsWorker"
        private const val UNIQUE_WORK_KEY = "DeleteStreamsWorkerUniqueKey"
        private const val SITES_LIMIT_GETTING = 100
        private var PROJECT_ID: String? = null

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<DeleteStreamsWorker>().setConstraints(constraints)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_KEY, ExistingWorkPolicy.REPLACE, workRequest)
        }

        fun enqueue(context: Context, projectId: String?) {
            PROJECT_ID = projectId ?: return enqueue(context)
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<DeleteStreamsWorker>().setConstraints(constraints)
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
