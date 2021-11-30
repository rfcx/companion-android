package org.rfcx.companion.util.file

import android.content.Context
import okhttp3.ResponseBody
import org.rfcx.companion.entity.GuardianSoftware
import org.rfcx.companion.entity.Software
import java.io.*
import java.lang.Exception

object APKUtils {

    private fun getAllDownloadedSoftwares(context: Context): Array<File>? {
        return File(context.filesDir, "guardian-software").listFiles()
    }

    fun getAllDownloadedSoftwaresWithType(context: Context): List<Software> {
        val softwares = mutableListOf<Software>()
        val files = getAllDownloadedSoftwaresVersion(context)
        files.forEach {
            when (it.key) {
                GuardianSoftware.ADMIN.value -> softwares.add(Software(
                    GuardianSoftware.ADMIN,
                    it.value.first,
                    it.value.second
                ))
                GuardianSoftware.CLASSIFY.value -> softwares.add(Software(
                    GuardianSoftware.CLASSIFY,
                    it.value.first,
                    it.value.second
                ))
                GuardianSoftware.GUARDIAN.value -> softwares.add(Software(
                    GuardianSoftware.GUARDIAN,
                    it.value.first,
                    it.value.second
                ))
                GuardianSoftware.UPDATER.value -> softwares.add(Software(
                    GuardianSoftware.UPDATER,
                    it.value.first,
                    it.value.second
                ))
            }
        }
        return softwares
    }

    fun getAllDownloadedSoftwaresVersion(context: Context): Map<String, Pair<String, String>> {
        val downloadedAPKs = getAllDownloadedSoftwares(context)
        val roleMappedVersion = mutableMapOf<String, Pair<String, String>>()
        if (downloadedAPKs.isNullOrEmpty()) {
            return roleMappedVersion
        }
        downloadedAPKs.forEach {
            val splitName = it.name.split("-")
            val role = splitName[0]
            val version = splitName[1].removeSuffix(".apk.gz")
            roleMappedVersion[role] = Pair(version, it.absolutePath)
        }
        return roleMappedVersion
    }

    fun compareVersionsIfNeedToUpdate(version1: String, version2: String?): Boolean {
        if (version2 == null) {
            return true
        }
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
            val file = File(dir, "$role-$version.apk.gz")
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

    fun getAPKFileFromPath(filePath: String): File {
        return File(filePath)
    }

    fun calculateVersionValue(versionName: String): Int {
        return try {
            val majorVersion = versionName.substring(0, versionName.indexOf(".")).toInt()
            val subVersion =
                versionName.substring(1 + versionName.indexOf("."), versionName.lastIndexOf("."))
                    .toInt()
            val updateVersion = versionName.substring(1 + versionName.lastIndexOf(".")).toInt()
            10000 * majorVersion + 100 * subVersion + updateVersion
        } catch (e: Exception) {
            0
        }
    }

    enum class APKStatus { NOT_INSTALLED, UP_TO_DATE, NEED_UPDATE }
}
