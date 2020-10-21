package org.rfcx.companion.service.profile

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.companion.entity.request.toRequestBody
import org.rfcx.companion.localdb.guardian.GuardianProfileDb
import org.rfcx.companion.repo.Firestore
import org.rfcx.companion.util.RealmHelper

class GuardianProfileSyncWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork")

        val db = GuardianProfileDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val firestore = Firestore(context)
        val profiles = db.lockUnsent()

        Log.d(TAG, "doWork: found ${profiles.size} unsent")
        var someFailed = false

        profiles.forEach {
            Log.d(TAG, "doWork: sending id ${it.id}")
            val result = firestore.sendProfile(it.toRequestBody())

            if (result != null) {
                Log.d(TAG, "$result")
                db.markSent(result.id, it.id)
            } else {
                db.markUnsent(it.id)
                someFailed = true
            }
        }

        return if (someFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "GProfileSyncWorker"
        private const val UNIQUE_WORK_KEY = "GProfileSyncWorkerUniqueKey"

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<GuardianProfileSyncWorker>().setConstraints(constraints)
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
