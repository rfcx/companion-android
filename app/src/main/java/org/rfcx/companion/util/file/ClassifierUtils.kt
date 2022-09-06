package org.rfcx.companion.util.file

import android.content.Context
import okhttp3.ResponseBody
import java.io.*

object ClassifierUtils {

    fun getAllDownloadedClassifiers(context: Context): Array<File>? {
        return File(context.filesDir, "classifiers").listFiles()
    }

    fun getDownloadedClassifierPath(context: Context, id: String): String {
        return context.filesDir.absolutePath + "/classifiers" + "/$id.tflite.gz"
    }

    fun classifierResponseToDisk(context: Context, body: ResponseBody, name: String): Boolean {
        return try {
            val dir = File(context.filesDir, "classifiers")
            if (!dir.exists()) {
                dir.mkdir()
            }
            val file = File(dir, "$name.tflite.gz")
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)
                while (true) {
                    val read: Int = inputStream.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                }
                outputStream.flush()
                true
            } catch (e: IOException) {
                false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            false
        }
    }

    fun deleteClassifier(context: Context, id: String): Boolean {
        val file = File(getDownloadedClassifierPath(context, id))
        if (!file.exists()) {
            return false
        }
        return file.delete()
    }
}
