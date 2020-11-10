package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.companion.entity.request.toRequestBody
import org.rfcx.companion.localdb.guardian.DiagnosticDb
import org.rfcx.companion.repo.Firestore
import org.rfcx.companion.util.RealmHelper

class DiagnosticSyncWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork")

        val db = DiagnosticDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val firestore = Firestore(context)
        val diagnostic = db.unlockSent()

        var someFailed = false
        Log.d(TAG, "doWork: found ${diagnostic.size} unsent")

        diagnostic.forEach {
            Log.d(TAG, "doWork: sending id ${it.id}")
            if (it.serverId == null) {
                val docRef = firestore.sendDiagnostic(it.toRequestBody())
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
                    firestore.updateDiagnostic(it.serverId!!, it.toRequestBody())
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
        private const val TAG = "DiagnosticSyncWorker"
        private const val UNIQUE_WORK_KEY = "DiagnosticSyncWorkerUniqueKey"

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<DiagnosticSyncWorker>().setConstraints(constraints)
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
