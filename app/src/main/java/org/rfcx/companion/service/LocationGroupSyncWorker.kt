package org.rfcx.companion.service

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.companion.entity.request.toRequestBody
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.repo.Firestore
import org.rfcx.companion.util.RealmHelper

class LocationGroupSyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val firestore = Firestore(appContext)

    override suspend fun doWork(): Result {
        val db = LocationGroupDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val locatesNeedToSync = db.unlockSent()
        var someFailed = false

        locatesNeedToSync.forEach {
            if (it.serverId != null) {
                if (it.deletedAt == null) {
                    try {
                        firestore.sendGroup(it.toRequestBody(), it.serverId!!)
                        db.markSent(it.serverId!!, it.id)
                    } catch (e: Exception) {
                        db.markUnsent(it.id)
                        someFailed = true
                    }
                } else {
                    try {
                        firestore.updateGroup(it.serverId!!, it.toRequestBody())
                        db.markSent(it.serverId!!, it.id)
                        
                        val isExisted = db.isExisted(it.name)
                        if (isExisted)
                            db.deleteLocationGroupFromLocal(it.id)
                    } catch (e: Exception) {
                        db.markUnsent(it.id)
                        someFailed = true
                    }
                }
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
