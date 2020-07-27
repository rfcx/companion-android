package org.rfcx.audiomoth.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.audiomoth.entity.request.toRequestBody
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.repo.Firestore
import org.rfcx.audiomoth.util.RealmHelper

class LocationSyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val firestore = Firestore(appContext)

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork")
        val db = LocateDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val locatesNeedToSync = db.unlockSent()
        var someFailed = false
        Log.d(TAG, "doWork: found ${locatesNeedToSync.size} unsent")
        locatesNeedToSync.forEach {
            Log.d(TAG, "doWork: sending id ${it.id}")

            if (it.serverId == null) {
                val docRef = firestore.sendLocation(it.toRequestBody())
                if (docRef != null) {
                    Log.d(TAG, "doWork:Create success ${it.id}")
                    db.markSent(docRef.id, it.id)
                } else {
                    Log.d(TAG, "doWork: Create failed ${it.id}")
                    db.markUnsent(it.id)
                    someFailed = true
                }
            } else {
                try {
                    firestore.updateLocation(it.serverId!!, it.toRequestBody())
                    db.markSent(it.serverId!!, it.id)
                    Log.d(TAG, "doWork:Update success ${it.id}")
                } catch (e: Exception) {
                    Log.d(TAG, "doWork:Update failed ${it.id}")
                    db.markUnsent(it.id)
                    someFailed = true
                }
            }
        }

        return if (someFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "LocationSyncWorker"
        private const val UNIQUE_WORK_KEY = "LocationSyncWorkerUniqueKey"

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<LocationSyncWorker>().setConstraints(constraints)
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
