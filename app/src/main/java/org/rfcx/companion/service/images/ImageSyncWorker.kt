package org.rfcx.companion.service.images

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import io.realm.Realm
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rfcx.companion.entity.request.toRequestBody
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.Storage
import org.rfcx.companion.util.getIdToken
import java.io.File

class ImageSyncWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork ImageSyncWorker")

        val db = DeploymentImageDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val deploymentImage = db.lockUnsent()

        Log.d(TAG, "doWork: found ${deploymentImage.size} unsent")
        var someFailed = false

        val token = "Bearer ${context.getIdToken()}"
        deploymentImage.forEach {
            val file = File(it.localPath)
            val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val result = ApiManager.getInstance().getDeviceApi()
                .uploadImage(token, it.deploymentServerId!!, body).execute()

            if (result.isSuccessful) {
                Log.d(TAG, "doWork: success $result")
                //TODO: get url back when uploaded
                db.markSent(it.id, null)
            } else {
                db.markUnsent(it.id)
                someFailed = true
            }
        }

        ImageSyncToFireStoreWorker.enqueue(context)

        return if (someFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "ImageSyncWorker"
        private const val UNIQUE_WORK_KEY = "ImageSyncWorkerUniqueKey"

        fun enqueue(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest =
                OneTimeWorkRequestBuilder<ImageSyncWorker>().setConstraints(constraints)
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
