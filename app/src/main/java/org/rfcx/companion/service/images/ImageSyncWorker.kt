package org.rfcx.companion.service.images

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.Storage

class ImageSyncWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork ImageSyncWorker")

        val db = DeploymentImageDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val storage = Storage(context)
        val deploymentImage = db.lockUnsent()

        Log.d(TAG, "doWork: found ${deploymentImage.size} unsent")
        var someFailed = false

        deploymentImage.forEach {
            val result = storage.sendImage(it.localPath)

            if (result != null) {
                Log.d(TAG, "doWork: success $result")
                db.markSent(it.id, result)
            } else {
                db.markUnsent(it.id)
                someFailed = true
            }
        }

        ImageSyncToFireStoreWorker.enqueue(context)

        return if (someFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "ImageSyncWorker"
        private const val UNIQUE_WORK_KEY = "ImageSyncWorkerUniqueKey"

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<ImageSyncWorker>().setConstraints(constraints)
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
