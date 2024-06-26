package org.rfcx.companion.service.images

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.realm.Realm
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.FileUtils.getMimeType
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.Storage
import java.io.File

class ImageSyncWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork ImageSyncWorker")

        val db = DeploymentImageDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val deploymentImage = db.lockUnsent()

        val storage = Storage(context)

        Log.d(TAG, "doWork: found ${deploymentImage.size} unsent")
        var someFailed = false

        deploymentImage.forEach {
            val file = File(it.localPath)
            val mimeType = file.getMimeType("image/jpeg")
            val requestFile = RequestBody.create(MediaType.parse(mimeType), storage.compressFile(context, file))
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val gson = Gson()
            val obj = JsonObject()
            obj.addProperty("label", it.imageLabel)
            val label = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(obj))
            val result = ApiManager.getInstance().getDeviceApi(context)
                .uploadAssets(it.deploymentServerId!!, body, label).execute()

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
