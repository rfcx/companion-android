package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.work.*
import io.realm.Realm
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.service.images.ImageSyncWorker
import org.rfcx.companion.util.RealmHelper
import java.util.concurrent.TimeUnit

class DeploymentCleanupWorker(val context: Context, params: WorkerParameters) :
    Worker(context, params) {

    override fun doWork(): Result {
        Log.d(TAG, "doWork")

        resendIfRequired()

        return Result.success()
    }

    private fun resendIfRequired() {
        val realm = Realm.getInstance(RealmHelper.migrationConfig())
        val edgeDeploymentDb = EdgeDeploymentDb(realm)
        val unsent = edgeDeploymentDb.unsentCount()
        Log.d(TAG, "resendIfRequired: found $unsent unsent")

        // In case any failed sending, we can resend (same ranger app)
        edgeDeploymentDb.unlockSending()
        if (unsent > 0) {
            DeploymentSyncWorker.enqueue(context)
        }

        val guardianDeploymentDb = GuardianDeploymentDb(realm)
        val guardianDeploymentUnsent = guardianDeploymentDb.unsentCount()
        guardianDeploymentDb.unlockSending()
        if (guardianDeploymentUnsent > 0) {
            GuardianDeploymentSyncWorker.enqueue(context)
        }

        val imageDb = DeploymentImageDb(realm)
        val imageUnsent = imageDb.unsentCount()
        imageDb.unlockSending()
        if (imageUnsent > 0) {
            ImageSyncWorker.enqueue(context)
        }

        DeleteStreamsWorker.enqueue(context)
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
