package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.work.*
import io.realm.Realm
import org.rfcx.companion.localdb.DeploymentDb
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.localdb.TrackingFileDb
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
        val deploymentDb = DeploymentDb(realm)
        val deploymentUnsent = deploymentDb.unsentCount()
        deploymentDb.unlockSending()
        if (deploymentUnsent > 0) {
            DeploymentSyncWorker.enqueue(context)
        }

        val imageDb = DeploymentImageDb(realm)
        val imageUnsent = imageDb.unsentCount()
        imageDb.unlockSending()
        if (imageUnsent > 0) {
            ImageSyncWorker.enqueue(context)
        }

        val trackingFileDb = TrackingFileDb(realm)
        val trackingFileUnsent = trackingFileDb.unsentCount()
        trackingFileDb.unlockSending()
        if (trackingFileUnsent > 0) {
            TrackingSyncWorker.enqueue(context)
        }
    }

    companion object {
        private const val TAG = "DeploymentCleanupWorker"
        private const val UNIQUE_WORK_KEY = "DeploymentCleanupWorkerUniqueKey"

        fun enqueuePeriodically(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            val workRequest =
                PeriodicWorkRequestBuilder<DeploymentCleanupWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_KEY,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun stopAllWork(context: Context) {
            WorkManager.getInstance(context).cancelAllWork()
        }
    }
}
