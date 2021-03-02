package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rfcx.companion.entity.response.StreamResponse
import org.rfcx.companion.entity.response.toLocate
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.getIdToken

class DeleteStreamsWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    var streams = listOf<StreamResponse>()
    var currentStreamsLoading = 0
    var someFailed = false

    override suspend fun doWork(): Result {
        //reset to default
        streams = listOf()
        currentStreamsLoading = 0
        someFailed = false

        Log.d(TAG, "doWork on DeleteStreams")

        val db = LocateDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val token = "Bearer ${context.getIdToken()}"
        val savedStreams = db.getLocations().filter { it.serverId != null }
        val result = getStreams(token, currentStreamsLoading)
        if (result) {
            val downloadedStreams = streams.map { it.toLocate().serverId }
            val filteredStreams = savedStreams.filter { stream -> !downloadedStreams.contains(stream.serverId) }
            filteredStreams.forEach {
                db.deleteLocate(it.id)
            }
        } else {
            someFailed = true
        }


        return if (someFailed) Result.retry() else Result.success()
    }

    private suspend fun getStreams(token: String, offset: Int): Boolean = withContext(Dispatchers.IO) {
        val result = ApiManager.getInstance().getDeviceApi()
            .getStreams(token, SITES_LIMIT_GETTING, offset).execute()
        if (result.isSuccessful) {
            val resultBody = result.body()
            resultBody?.let {
                streams = streams + it
                if (it.size == SITES_LIMIT_GETTING) {
                    currentStreamsLoading += SITES_LIMIT_GETTING
                    return@withContext getStreams(token, currentStreamsLoading)
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

        fun enqueue(context: Context) {
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
