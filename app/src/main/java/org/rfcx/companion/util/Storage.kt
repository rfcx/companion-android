package org.rfcx.companion.util

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import me.echodev.resizer.Resizer
import java.io.File

class Storage(val context: Context) {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

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

            uploadTask?.continueWithTask(
                Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    return@Continuation ref.downloadUrl
                }
            )?.addOnCompleteListener { task ->
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

    /** compress imagePath to less than 1 MB **/
    fun compressFile(context: Context?, file: File): File {
        if (file.length() <= 0) {
            return file
        }
        val resized = Resizer(context)
            .setTargetLength(1920)
            .setQuality(80)
            .setSourceImage(file)
            .resizedFile
        return if (resized.length() > 1000000) {
            compressFile(context, resized)
        } else {
            resized
        }
    }
}
