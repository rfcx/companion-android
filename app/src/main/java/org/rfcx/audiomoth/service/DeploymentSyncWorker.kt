package org.rfcx.audiomoth.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.audiomoth.entity.request.toRequestBody
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.repo.Firestore
import org.rfcx.audiomoth.service.images.ImageSyncWorker
import org.rfcx.audiomoth.util.RealmHelper

/**
 * For syncing data to server. Ref from Ranger Android App
 */
class DeploymentSyncWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork")

        val db = DeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val locateDb = LocateDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val firestore = Firestore(context)
        val deployments = db.lockUnsent()

        Log.d(TAG, "doWork: found ${deployments.size} unsent")
        var someFailed = false

        deployments.forEach {
            Log.d(TAG, "doWork: sending id ${it.id}")
            val result = firestore.sendDeployment(it.toRequestBody())

            if (result != null) {
                Log.d(TAG, "doWork: success ${it.id}")
                db.markSent(result.id, it.id)
                locateDb.updateDeploymentServerId(it.id, result.id)
            } else {
                Log.d(TAG, "doWork: failed ${it.id}")
                db.markUnsent(it.id)
                someFailed = true
            }
        }

        ImageSyncWorker.enqueue(context)
        LocationSyncWorker.enqueue(context)

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