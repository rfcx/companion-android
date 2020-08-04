package org.rfcx.audiomoth.service

import android.content.Context
import android.util.Log
import androidx.work.*
import io.realm.Realm
import java.util.concurrent.TimeUnit
import org.rfcx.audiomoth.localdb.EdgeDeploymentDb
import org.rfcx.audiomoth.localdb.ProfileDb
import org.rfcx.audiomoth.localdb.guardian.GuardianDeploymentDb
import org.rfcx.audiomoth.localdb.guardian.GuardianProfileDb
import org.rfcx.audiomoth.service.profile.GuardianProfileSyncWorker
import org.rfcx.audiomoth.service.profile.ProfileSyncWorker
import org.rfcx.audiomoth.util.RealmHelper

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

        val profileDb = ProfileDb(realm)
        val profileUnsent = profileDb.unsentCount()
        profileDb.unlockSending()
        if (profileUnsent > 0) {
            ProfileSyncWorker.enqueue(context)
        }

        val guardianDeploymentDb = GuardianDeploymentDb(realm)
        val guardianDeploymentUnsent = guardianDeploymentDb.unsentCount()
        guardianDeploymentDb.unlockSending()
        if (guardianDeploymentUnsent > 0) {
            GuardianDeploymentSyncWorker.enqueue(context)
        }

        val guardianProfileDb = GuardianProfileDb(realm)
        val guardianProfileUnsent = guardianProfileDb.unsentCount()
        guardianProfileDb.unlockSending()
        if (guardianProfileUnsent > 0) {
            GuardianProfileSyncWorker.enqueue(context)
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
