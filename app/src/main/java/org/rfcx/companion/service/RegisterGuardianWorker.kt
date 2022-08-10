package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import org.rfcx.companion.localdb.GuardianRegistrationDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.getIdToken

class RegisterGuardianWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork RegisterGuardianWorker")

        val db = GuardianRegistrationDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val registrations = db.getAll()

        Log.d(TAG, "doWork: found ${registrations?.size ?: 0} unsent")
        var someFailed = false

        val token = "Bearer ${context.getIdToken()}"
        registrations?.forEach {
            val result = ApiManager.getInstance().getDeviceApi2()
                .registerGuardian(token, it.toRequest()).execute()

            if (result.isSuccessful) {
                db.delete(it.guid)
                Log.d(TAG, "doWork: success $result")
            } else {
                someFailed = true
            }
        }

        return if (someFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "RegisterGuardianWorker"
        private const val UNIQUE_WORK_KEY = "RegisterGuardianWorkerUniqueKey"

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<RegisterGuardianWorker>().setConstraints(constraints)
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
