package org.rfcx.companion.util

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.File
import kotlinx.coroutines.tasks.await

class Storage(val context: Context) {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val preferences = Preferences.getInstance(context)
    private val guid = preferences.getString(Preferences.USER_GUID, "images")

    suspend fun sendImage(uri: String): String? {
        val file = Uri.fromFile(File(uri))
        val ref = file.lastPathSegment?.let { storageRef.child("$guid/${file.lastPathSegment}") }
        val uploadTask = ref?.putFile(file)
        var uriDownload: String? = null
        uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        })?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                uriDownload = downloadUri.toString()
            }
        }?.await()

        return uriDownload
    }

    fun uploadImagesOfFeedback(
        uris: List<String>,
        callback: (Boolean, ArrayList<String>?) -> Unit
    ) {
        var counter = 0
        val pathImages = ArrayList<String>()

        uris.forEach { pathName ->
            val file = Uri.fromFile(File(pathName))
            val ref = file.lastPathSegment?.let { storageRef.child(file.lastPathSegment!!) }
            val uploadTask = ref?.putFile(file)

            uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                return@Continuation ref.downloadUrl
            })?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    counter += 1

                    val downloadUri = task.result
                    pathImages.add(downloadUri.toString())

                    if (counter == uris.size) {
                        callback(true, pathImages)
                    }
                } else {
                    callback(false, null)
                }
            }
        }
    }
}
