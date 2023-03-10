package org.rfcx.companion.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rfcx.companion.localdb.TrackingFileDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.FileUtils.getMimeType
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.getIdToken
import java.io.File

class TrackingSyncWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork")

        val db = TrackingFileDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val tracking = db.lockUnsent()

        var someFailed = false
        Log.d(TAG, "doWork: found ${tracking.size} unsent")

        val token = "Bearer ${context.getIdToken()}"
        tracking.forEach {
            val file = File(it.localPath)
            val mimeType = file.getMimeType()
            val requestFile = RequestBody.create(MediaType.parse(mimeType), file)
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val result = ApiManager.getInstance().getDeviceApi(context)
                .uploadAssets(token, it.deploymentServerId!!, body).execute()

            if (result.isSuccessful) {
                val assetPath = result.headers().get("Location")
                assetPath?.let { path ->
                    db.markSent(it.id, path.substring(1, path.length))
                }

                Log.d(TAG, "doWork: success $result")
            } else {
                db.markUnsent(it.id)
                someFailed = true
            }
        }

        return if (someFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "TrackingSyncWorker"
        private const val UNIQUE_WORK_KEY = "TrackingSyncWorkerUniqueKey"

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<TrackingSyncWorker>().setConstraints(constraints)
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
