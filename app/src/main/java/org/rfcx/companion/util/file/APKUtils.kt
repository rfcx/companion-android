package org.rfcx.companion.util.file

import android.content.Context
import java.io.File

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

    enum class APKStatus { NOT_INSTALLED, UP_TO_DATE, NEED_UPDATE }
}
