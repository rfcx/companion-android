package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rfcx.companion.entity.Device
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.TrackingFileDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.geojson.GeoJsonUtils
import org.rfcx.companion.util.getIdToken

class DownloadAssetsWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private var count = 0
    private var currentStreamsLoading = 0
    private var someFailed = false

    override suspend fun doWork(): Result {
        //reset to default
        count = 0
        currentStreamsLoading = 0
        someFailed = false

        Log.d(TAG, "doWork on DownloadAssets")

        val edgeDeploymentDb = EdgeDeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
        edgeDeploymentDb.getAll().forEach { dp ->
            val token = "Bearer ${context.getIdToken()}"
            val result = ApiManager.getInstance().getDeviceApi().getAssets(token, dp.serverId!!).execute()
            if (result.isSuccessful) {
                val dpAssets = result.body()
                dpAssets?.forEach { item ->
                    if (item.mimeType.startsWith("image")) {
                        val deploymentImageDb = DeploymentImageDb(Realm.getInstance(RealmHelper.migrationConfig()))
                        deploymentImageDb.insertOrUpdate(
                            item,
                            dp.id,
                            Device.AUDIOMOTH.value
                        )
                    } else if(item.mimeType.endsWith("geo+json")) {
                        val trackingFileDb = TrackingFileDb(Realm.getInstance(RealmHelper.migrationConfig()))
                        val filePath = GeoJsonUtils.downloadGeoJsonFile(
                            context,
                            token,
                            item,
                            dp.serverId!!,
                            dp.deployedAt
                        )
                        if (filePath != null) {
                            trackingFileDb.insertOrUpdate(
                                item,
                                filePath,
                                dp.id,
                                Device.AUDIOMOTH.value
                            )
                        }
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

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<DownloadAssetsWorker>().setConstraints(constraints)
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
