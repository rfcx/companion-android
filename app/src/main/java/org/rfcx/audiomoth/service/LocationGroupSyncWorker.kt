package org.rfcx.audiomoth.service

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.audiomoth.entity.request.toRequestBody
import org.rfcx.audiomoth.localdb.LocationGroupDb
import org.rfcx.audiomoth.repo.Firestore
import org.rfcx.audiomoth.util.RealmHelper

class LocationGroupSyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val firestore = Firestore(appContext)

    override suspend fun doWork(): Result {
        val db = LocationGroupDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val locatesNeedToSync = db.unlockSent()
        var someFailed = false

        locatesNeedToSync.forEach {
            val docRef = firestore.sendGroup(it.toRequestBody())
            if (docRef != null) {
                db.markSent(docRef.id, it.id)
            } else {
                db.markUnsent(it.id)
                someFailed = true
            }
        }

        return if (someFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "LocationGroupSyncWorker"
        private const val UNIQUE_WORK_KEY = "LocationGroupSyncWorkerUniqueKey"

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<LocationGroupSyncWorker>().setConstraints(constraints)
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
