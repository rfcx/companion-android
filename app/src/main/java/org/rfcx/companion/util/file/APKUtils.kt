package org.rfcx.companion.util.file

import android.content.Context
import android.util.Log
import okhttp3.ResponseBody
import java.io.*

object APKUtils {

    fun getAllDownloadedSoftwares(context: Context): Array<File>? {
        return File(context.filesDir, "guardian-software").listFiles()
    }

    fun getAllDownloadedSoftwaresVersion(context: Context): Map<String, String> {
        val downloadedAPKs = getAllDownloadedSoftwares(context)
        val roleMappedVersion = mutableMapOf<String, String>()
        if (downloadedAPKs.isNullOrEmpty()) {
            return roleMappedVersion
        }
        downloadedAPKs.forEach {
            val splitName = it.name.split("-")
            val role = splitName[0]
            val version = it.name.split("-")[1]
            roleMappedVersion[role] = version
        }
        Log.d("SSSS", downloadedAPKs.contentToString())
        return roleMappedVersion
    }

    fun compareVersionsIfNeedToUpdate(version1: String, version2: String): Boolean {
        val levels1 = version1.split("\\.")
        val levels2 = version2.split("\\.")
        val length = levels1.size.coerceAtLeast(levels2.size)
        for (i in 0 until length) {
            val v1 = if (i < levels1.size) levels1[i].toInt() else 0
            val v2 = if (i < levels2.size) levels2[i].toInt() else 0
            val compare = v1.compareTo(v2)
            if (compare < 0) {
                return true
            }
        }
        return false
    }

    fun apkResponseToDisk(context: Context, body: ResponseBody, role: String, version: String): Boolean {
        return try {
            val dir = File(context.filesDir, "guardian-software")
            if (!dir.exists()) {
                dir.mkdir()
            }
            val file = File(dir, "$role-$version-release.apk.gz")
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

    enum class APKStatus { NOT_INSTALLED, UP_TO_DATE, NEED_UPDATE }
}
