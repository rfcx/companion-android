package org.rfcx.audiomoth.util

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.File

class Storage(val context: Context) {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val preferences = Preferences.getInstance(context)
    private val guid = preferences.getString(Preferences.USER_GUID, "images")

    fun uploadImage(uris: List<String>, deploymentId: String, callback: (Int, Int) -> Unit) {
        var count = uris.size
        val pathImages = arrayListOf<String>()

        uris.forEach {
            callback(uris.size, count)

            val file = Uri.fromFile(File(it))

            val ref =
                file.lastPathSegment?.let {
                    storageRef.child("$guid/${file.lastPathSegment}")
                }
            val uploadTask = ref?.putFile(file)

            uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                return@Continuation ref.downloadUrl
            })?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    count -= 1
                    val downloadUri = task.result
                    callback(uris.size, count)
                    pathImages.add(downloadUri.toString())
                    if (count == 0) {
                        Firestore(context).updateDeployment(guid, deploymentId, pathImages)
                    }
                }
            }
        }
    }
}