package org.rfcx.audiomoth.service

import android.content.Context
import android.util.Log
import androidx.work.*
import io.realm.Realm
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.util.RealmHelper
import java.util.concurrent.TimeUnit

class DeploymentCleanupWorker(val context: Context, params: WorkerParameters) :
    Worker(context, params) {

    override fun doWork(): Result {
        Log.d(TAG, "doWork")

        resendIfRequired()

        return Result.success()
    }

    private fun resendIfRequired() {
        val db = DeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val unsent = db.unsentCount()
        Log.d(TAG, "resendIfRequired: found $unsent unsent")

        // In case any failed sending, we can resend (same ranger app)
        db.unlockSending()
        if (unsent > 0) {
            DeploymentSyncWorker.enqueue(context)
        }
    }

    companion object {
        private const val TAG = "DeploymentCleanupWorker"
        private const val UNIQUE_WORK_KEY = "DeploymentCleanupWorkerUniqueKey"

        fun enqueuePeriodically(context: Context) {
            val workRequest =
                PeriodicWorkRequestBuilder<DeploymentCleanupWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_KEY,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}