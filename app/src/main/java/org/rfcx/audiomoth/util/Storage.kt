package org.rfcx.audiomoth.util

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.File

class Storage(context: Context) {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val preferences = Preferences.getInstance(context)
    private val guid = preferences.getString(Preferences.USER_GUID, "images")

    fun uploadImage(uris: List<String>) {
        uris.forEach {
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
                    val downloadUri = task.result
                }
            }
        }
    }
}