package org.rfcx.audiomoth.service.images

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.audiomoth.entity.request.toRequestBody
import org.rfcx.audiomoth.localdb.DeploymentImageDb
import org.rfcx.audiomoth.repo.Firestore
import org.rfcx.audiomoth.service.LocationSyncWorker
import org.rfcx.audiomoth.util.RealmHelper

class ImageSyncToFireStoreWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val fireStore = Firestore(context)
    override suspend fun doWork(): Result {
        val db = DeploymentImageDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val deploymentImages = db.lockUnsentForFireStore()

        var someFailed = false
        deploymentImages.forEach { deploymentImage ->
            val result = fireStore.sendImage(deploymentImage.toRequestBody())
            if (result != null) {
                Log.d(TAG, "doWork: success $result")
                db.markSentFireStore(deploymentImage.id)
            } else {
                db.markUnsentFireStore(deploymentImage.id)
                someFailed = true
            }
            someFailed = result == null
        }

        LocationSyncWorker.enqueue(context)

        return if (someFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "ImageFireStoreWorker"
        private const val UNIQUE_WORK_KEY = "ImageSyncToFireStoreWorkerUniqueKey"

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<ImageSyncToFireStoreWorker>().setConstraints(constraints)
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
